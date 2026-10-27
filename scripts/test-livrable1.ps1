<#
    Script de test bout-en-bout du LIVRABLE 1 (départements, employés, contrats)
    Projet AtlasRH - API http://localhost:8080

    Le script est rejouable :
      - il nettoie ses propres données avant de commencer (EMP009, contrats de test
        d'EMP004, départements Marketing) ;
      - il recrée ensuite ses données avec les valeurs attendues par le brief.

    Il utilise curl.exe avec des fichiers JSON (pas l'alias curl de PowerShell)
    et lit les réponses en UTF-8 pour éviter les problèmes d'accents/guillemets.

    Usage :  powershell -ExecutionPolicy Bypass -File .\scripts\test-livrable1.ps1
#>

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

# Affichage correct des accents dans la console (Windows PowerShell 5.1)
try {
    [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
    $OutputEncoding = [Console]::OutputEncoding
} catch { }

# ---------------------------------------------------------------------------
# Infrastructure du script
# ---------------------------------------------------------------------------
$runId  = Get-Date -Format "yyyyMMddHHmmss"
$tmpDir = Join-Path $env:TEMP "atlasrh-tests-$runId"
New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null

$script:Results = @()
$script:TestNo  = 0
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-JsonFile {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        $Body = $null
    )

    $respFile = Join-Path $tmpDir ("resp-" + [guid]::NewGuid().ToString("N") + ".json")
    $curlArgs = @("-s", "-o", $respFile, "-w", "%{http_code}", "-X", $Method, "$BaseUrl$Path")

    if ($null -ne $Body) {
        $bodyFile = Join-Path $tmpDir ("body-" + [guid]::NewGuid().ToString("N") + ".json")
        $json = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 8 -Compress }
        Write-JsonFile -Path $bodyFile -Content $json
        $curlArgs += @("-H", "Content-Type: application/json; charset=utf-8", "--data-binary", "@$bodyFile")
    }

    $statusText = (& curl.exe @curlArgs 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "curl.exe a échoué (code $LASTEXITCODE) pour $Method $Path. L'API est-elle démarrée sur $BaseUrl ?"
    }

    $raw = ""
    if (Test-Path $respFile) {
        $raw = [System.IO.File]::ReadAllText($respFile, [System.Text.Encoding]::UTF8)
    }

    $parsed = $null
    if ($raw -and $raw.Trim().Length -gt 0) {
        try { $parsed = $raw | ConvertFrom-Json } catch { $parsed = $null }
    }

    return [pscustomobject]@{
        Status = [int]$statusText
        Raw    = $raw
        Json   = $parsed
    }
}

function Add-Test {
    param(
        [string]$Name,
        [string]$Expected,
        [string]$Obtained,
        [bool]$OK
    )
    $script:TestNo++
    $script:Results += [pscustomobject]@{
        No       = $script:TestNo
        Test     = $Name
        Expected = $Expected
        Obtained = $Obtained
        OK       = if ($OK) { "OK" } else { "KO" }
    }
}

function Get-ErrorMessage {
    param($Response)
    if ($null -eq $Response.Json) { return "" }
    if ($Response.Json.PSObject.Properties.Name -contains "message") { return [string]$Response.Json.message }
    return ""
}

# Vérifie le format JSON d'erreur unique : timestamp, status, error, message, path
# (+ errors pour les erreurs de validation Bean Validation).
# Retourne $null si conforme, sinon une chaîne décrivant le problème.
function Test-ErrorFormat {
    param($Response, [bool]$RequireErrors = $false)

    if ($null -eq $Response.Json) { return "corps JSON absent/illisible" }
    $props = @($Response.Json.PSObject.Properties.Name)
    $missing = @()
    foreach ($field in @("timestamp", "status", "error", "message", "path")) {
        if ($props -notcontains $field) { $missing += $field }
    }
    if ($missing.Count -gt 0) { return "champs manquants : " + ($missing -join ", ") }
    if ([int]$Response.Json.status -ne $Response.Status) {
        return "status JSON=$($Response.Json.status) différent du HTTP=$($Response.Status)"
    }
    if ($RequireErrors) {
        if ($props -notcontains "errors") { return "champ 'errors' manquant" }
        $errCount = @($Response.Json.errors.PSObject.Properties).Count
        if ($errCount -eq 0) { return "objet 'errors' vide" }
    }
    return $null
}

function Format-ErrorObtained {
    param($Response, $FormatIssue)
    $msg = Get-ErrorMessage -Response $Response
    if ($msg.Length -gt 110) { $msg = $msg.Substring(0, 107) + "..." }
    $suffix = if ($FormatIssue) { " ; FORMAT KO: $FormatIssue" } else { " ; format JSON OK" }
    return "$($Response.Status) - $msg$suffix"
}

$apiUp = $false
try {
    $ping = Invoke-Api -Method "GET" -Path "/api/departments"
    $apiUp = ($ping.Status -eq 200)
} catch {
    Write-Host "ERREUR : $($_.Exception.Message)" -ForegroundColor Red
    exit 2
}
if (-not $apiUp) {
    Write-Host "ERREUR : GET /api/departments ne répond pas 200. Arrêt." -ForegroundColor Red
    exit 2
}

# ---------------------------------------------------------------------------
# Nettoyage des données laissées par une exécution précédente
# ---------------------------------------------------------------------------
Write-Host "Nettoyage des données de test précédentes..." -ForegroundColor Cyan
$cleanupSql = @"
DELETE FROM contract WHERE employee_id IN (SELECT id FROM employee WHERE matricule = 'EMP009' OR matricule LIKE 'EMPX%');
DELETE FROM employee WHERE matricule = 'EMP009' OR matricule LIKE 'EMPX%';
DELETE FROM contract c USING employee e
 WHERE c.employee_id = e.id AND e.matricule = 'EMP004' AND c.start_date >= DATE '2026-01-01';
UPDATE contract c SET status = 'EN_COURS', end_date = NULL
  FROM employee e
 WHERE c.employee_id = e.id AND e.matricule = 'EMP004' AND c.start_date = DATE '2022-01-01';
UPDATE employee SET manager_id = (SELECT id FROM employee WHERE matricule = 'EMP001')
 WHERE matricule = 'EMP004';
DELETE FROM department d
 WHERE d.name IN ('Marketing', 'Marketing Test')
   AND NOT EXISTS (SELECT 1 FROM employee e WHERE e.department_id = d.id);
"@
$cleanupSql | docker exec -i atlasrh-db psql -U atlasrh -d atlasrh -v ON_ERROR_STOP=1 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Nettoyage SQL impossible (docker exec code $LASTEXITCODE). Les tests risquent d'échouer sur des données résiduelles."
}

# ---------------------------------------------------------------------------
# Résolution des identifiants réels
# ---------------------------------------------------------------------------
$allEmployees = Invoke-Api -Method "GET" -Path "/api/employees?size=200&sort=id,asc"
if ($allEmployees.Status -ne 200) {
    Write-Host "ERREUR : impossible de lister les employés. Arrêt." -ForegroundColor Red
    exit 2
}
$empIds = @{}
foreach ($e in $allEmployees.Json.content) { $empIds[$e.matricule] = $e.id }

$departments0 = Invoke-Api -Method "GET" -Path "/api/departments?size=200"
$deptIds = @{}
foreach ($d in $departments0.Json.content) { $deptIds[$d.name] = $d.id }

$idEmp004 = $empIds["EMP004"]
$idEmp005 = $empIds["EMP005"]
$idEmp002 = $empIds["EMP002"]
$idEmp001 = $empIds["EMP001"]
$idDeptInfo = $deptIds["Informatique"]
$idDeptFinance = $deptIds["Finance"]

if (-not $idEmp004 -or -not $idEmp005 -or -not $idEmp002 -or -not $idEmp001 -or -not $idDeptInfo) {
    Write-Host "ERREUR : les données de démo attendues (EMP001..EMP008, départements) sont absentes. Arrêt." -ForegroundColor Red
    exit 2
}

$today = (Get-Date).ToString("yyyy-MM-dd")
$matPrefix = "EMPX" + $runId.Substring($runId.Length - 6)
$emailPrefix = "test" + $runId.Substring($runId.Length - 6)

# Corps de base d'EMP004 (relu depuis l'API pour les PUT)
$emp004 = Invoke-Api -Method "GET" -Path "/api/employees/$idEmp004"
$emp004Base = @{
    matricule    = $emp004.Json.matricule
    lastName     = $emp004.Json.lastName
    firstName    = $emp004.Json.firstName
    email        = $emp004.Json.email
    position     = $emp004.Json.position
    hireDate     = $emp004.Json.hireDate
    departmentId = $emp004.Json.departmentId
}

# ===========================================================================
# 1. DÉPARTEMENTS
# ===========================================================================
Write-Host "Tests départements..." -ForegroundColor Cyan

$r = Invoke-Api -Method "GET" -Path "/api/departments"
$count = if ($r.Json) { $r.Json.totalElements } else { "?" }
$hasPage = ($r.Json -and ($r.Json.PSObject.Properties.Name -contains "content") -and ($r.Json.PSObject.Properties.Name -contains "totalElements"))
Add-Test "GET /api/departments" "200 + réponse paginée + 3 départements" `
    "$($r.Status) + paginée=$hasPage + totalElements=$count" `
    ($r.Status -eq 200 -and $hasPage -and $count -eq 3)

$marketingBody = @{ name = "Marketing"; description = "Communication" }
$r = Invoke-Api -Method "POST" -Path "/api/departments" -Body $marketingBody
$marketingId = if ($r.Json) { $r.Json.id } else { $null }
Add-Test "POST /api/departments Marketing" "201 + id renvoyé" `
    "$($r.Status) + id=$marketingId + name=$($r.Json.name)" `
    ($r.Status -eq 201 -and $null -ne $marketingId -and $r.Json.name -eq "Marketing")

$r = Invoke-Api -Method "POST" -Path "/api/departments" -Body $marketingBody
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST /api/departments Marketing (doublon)" "409 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 409 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/departments" -Body @{ name = ""; description = "x" }
$fmt = Test-ErrorFormat -Response $r -RequireErrors $true
Add-Test "POST /api/departments nom vide" "400 + champ errors" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$r = Invoke-Api -Method "PUT" -Path "/api/departments/$marketingId" -Body @{ name = "Marketing Test"; description = "Communication" }
Add-Test "PUT département Marketing (nouveau nom)" "200 + nom mis à jour" `
    "$($r.Status) + name=$($r.Json.name)" `
    ($r.Status -eq 200 -and $r.Json.name -eq "Marketing Test")

$r = Invoke-Api -Method "PUT" -Path "/api/departments/$marketingId" -Body @{ name = "Finance"; description = "x" }
$fmt = Test-ErrorFormat -Response $r
Add-Test "PUT département Marketing -> nom Finance" "409 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 409 -and $null -eq $fmt)

$r = Invoke-Api -Method "PUT" -Path "/api/departments/999" -Body @{ name = "Inexistant"; description = "x" }
$fmt = Test-ErrorFormat -Response $r
Add-Test "PUT /api/departments/999" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

$r = Invoke-Api -Method "GET" -Path "/api/departments/$marketingId"
Add-Test "GET /api/departments/{id Marketing}" "200 + département Marketing Test" `
    "$($r.Status) + name=$($r.Json.name)" `
    ($r.Status -eq 200 -and $r.Json.name -eq "Marketing Test")

$r = Invoke-Api -Method "GET" -Path "/api/departments/999"
$fmt = Test-ErrorFormat -Response $r
Add-Test "GET /api/departments/999" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

$r = Invoke-Api -Method "DELETE" -Path "/api/departments/$idDeptInfo"
$fmt = Test-ErrorFormat -Response $r
Add-Test "DELETE département Informatique (non vide)" "409 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 409 -and $null -eq $fmt)

$r = Invoke-Api -Method "DELETE" -Path "/api/departments/$marketingId"
$hasMessage = ($r.Json -and ($r.Json.PSObject.Properties.Name -contains "message") -and $r.Json.message)
Add-Test "DELETE département Marketing (vide)" "200 + message JSON" `
    "$($r.Status) + message=$hasMessage" `
    ($r.Status -eq 200 -and [bool]$hasMessage)

$r = Invoke-Api -Method "GET" -Path "/api/departments/$marketingId"
$fmt = Test-ErrorFormat -Response $r
Add-Test "GET département Marketing supprimé" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

# ===========================================================================
# 2. EMPLOYÉS
# ===========================================================================
Write-Host "Tests employés..." -ForegroundColor Cyan

$r = Invoke-Api -Method "GET" -Path "/api/employees?search=ala"
$names = @($r.Json.content | ForEach-Object { "$($_.firstName) $($_.lastName)" })
Add-Test "GET /api/employees?search=ala" "200 + contient Alaoui" `
    "200 + $($names.Count) résultat(s) : $($names -join ', ')" `
    ($r.Status -eq 200 -and (($names -join " ") -match "Alaoui"))

$r = Invoke-Api -Method "GET" -Path "/api/employees?search=ben"
$names = @($r.Json.content | ForEach-Object { "$($_.firstName) $($_.lastName)" })
Add-Test "GET /api/employees?search=ben (contrôle recherche)" "200 + contient Benali" `
    "200 + $($names.Count) résultat(s) : $($names -join ', ')" `
    ($r.Status -eq 200 -and (($names -join " ") -match "Benali"))

$r = Invoke-Api -Method "GET" -Path "/api/employees"
Add-Test "GET /api/employees (sans paramètre)" "200 + page d'employés" `
    "$($r.Status) + totalElements=$($r.Json.totalElements)" `
    ($r.Status -eq 200 -and $r.Json.PSObject.Properties.Name -contains "content")

$r = Invoke-Api -Method "GET" -Path "/api/employees?search="
$isPgError = ($r.Status -eq 500)
Add-Test "GET /api/employees?search= (chaîne vide)" "200, aucune erreur PostgreSQL" `
    "$($r.Status) + totalElements=$($r.Json.totalElements)" `
    ($r.Status -eq 200 -and -not $isPgError)

$r = Invoke-Api -Method "GET" -Path "/api/employees?departmentId=$idDeptInfo&sort=lastName,asc"
$deptOk = $true
foreach ($e in $r.Json.content) { if ($e.departmentId -ne $idDeptInfo) { $deptOk = $false } }
$lastNames = @($r.Json.content | ForEach-Object { $_.lastName })
$sortedExpected = @($lastNames | Sort-Object)
$sortOk = (($lastNames -join "|") -eq ($sortedExpected -join "|"))
Add-Test "GET employés département Informatique triés lastName asc" "200 + uniquement dept 2 + tri croissant" `
    "$($r.Status) + deptOK=$deptOk + triOK=$sortOk + $($lastNames -join ', ')" `
    ($r.Status -eq 200 -and $deptOk -and $sortOk)

$r = Invoke-Api -Method "GET" -Path "/api/employees?sort=abc"
$fmt = Test-ErrorFormat -Response $r
Add-Test "GET /api/employees?sort=abc" "400 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$emp009Body = @{
    matricule    = "EMP009"
    lastName     = "Test"
    firstName    = "Gabi"
    email        = "gabi.test@atlasrh.ma"
    position     = "Développeur"
    hireDate     = "2026-10-01"
    departmentId = $idDeptInfo
    managerId    = $idEmp002
}
$r = Invoke-Api -Method "POST" -Path "/api/employees" -Body $emp009Body
$emp009Id = if ($r.Json) { $r.Json.id } else { $null }
Add-Test "POST employé EMP009 valide" "201 + id + matricule EMP009" `
    "$($r.Status) + id=$emp009Id + matricule=$($r.Json.matricule) + managerId=$($r.Json.managerId)" `
    ($r.Status -eq 201 -and $null -ne $emp009Id -and $r.Json.matricule -eq "EMP009" -and $r.Json.managerId -eq $idEmp002)

$r = Invoke-Api -Method "POST" -Path "/api/employees" -Body @{
    matricule = "EMP009"; lastName = "Doublon"; firstName = "X"; email = "$emailPrefix-dup@atlasrh.ma"
    position = "Test"; hireDate = "2026-10-01"; departmentId = $idDeptInfo
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST employé matricule EMP009 déjà utilisé" "409 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 409 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees" -Body @{
    matricule = "EMP010"; lastName = "DoublonEmail"; firstName = "X"; email = "gabi.test@atlasrh.ma"
    position = "Test"; hireDate = "2026-10-01"; departmentId = $idDeptInfo
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST employé e-mail déjà utilisé (autre matricule)" "409 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 409 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees" -Body @{
    matricule = "$matPrefix-1"; lastName = "Dept"; firstName = "Inconnu"; email = "$emailPrefix-1@atlasrh.ma"
    position = "Test"; hireDate = "2026-10-01"; departmentId = 999
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST employé departmentId=999" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees" -Body @{
    matricule = "$matPrefix-2"; lastName = "Manager"; firstName = "Inconnu"; email = "$emailPrefix-2@atlasrh.ma"
    position = "Test"; hireDate = "2026-10-01"; departmentId = $idDeptInfo; managerId = 999
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST employé managerId=999" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees" -Body @{
    matricule = "$matPrefix-3"; lastName = "Email"; firstName = "Invalide"; email = "pas-un-email"
    position = "Test"; hireDate = "2026-10-01"; departmentId = $idDeptInfo
}
$fmt = Test-ErrorFormat -Response $r -RequireErrors $true
Add-Test "POST employé e-mail invalide" "400 + champ errors" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees" -Body @{
    matricule = "$matPrefix-4"; lastName = "SansDate"; firstName = "Test"; email = "$emailPrefix-4@atlasrh.ma"
    position = "Test"; departmentId = $idDeptInfo
}
$fmt = Test-ErrorFormat -Response $r -RequireErrors $true
Add-Test "POST employé sans hireDate" "400 + champ errors" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees" -Body @{
    matricule = "$matPrefix-5"; lastName = "Date"; firstName = "Invalide"; email = "$emailPrefix-5@atlasrh.ma"
    position = "Test"; hireDate = "pas-une-date"; departmentId = $idDeptInfo
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST employé hireDate invalide" "400 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$selfBody = $emp004Base.Clone()
$selfBody["managerId"] = $idEmp004
$r = Invoke-Api -Method "PUT" -Path "/api/employees/$idEmp004" -Body $selfBody
$fmt = Test-ErrorFormat -Response $r
Add-Test "PUT EMP004 avec managerId = son propre id" "400 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$r = Invoke-Api -Method "PUT" -Path "/api/employees/$idEmp004" -Body $emp004Base
Add-Test "PUT EMP004 sans managerId" "200 + managerId null" `
    "$($r.Status) + managerId=$($r.Json.managerId)" `
    ($r.Status -eq 200 -and $null -eq $r.Json.managerId)

$restoreBody = $emp004Base.Clone()
$restoreBody["managerId"] = $idEmp001
$r = Invoke-Api -Method "PUT" -Path "/api/employees/$idEmp004" -Body $restoreBody
Add-Test "PUT EMP004 remise du manager d'origine" "200 + managerId=$idEmp001" `
    "$($r.Status) + managerId=$($r.Json.managerId)" `
    ($r.Status -eq 200 -and $r.Json.managerId -eq $idEmp001)

$r = Invoke-Api -Method "GET" -Path "/api/employees/$idEmp002/team"
$teamMat = @($r.Json.content | ForEach-Object { $_.matricule })
$briefTeam = @("EMP004", "EMP005", "EMP008")
$briefTeamOk = $true
foreach ($m in $briefTeam) { if ($teamMat -notcontains $m) { $briefTeamOk = $false } }
if ($emp009Id -and ($teamMat -notcontains "EMP009")) { $briefTeamOk = $false }
Add-Test "GET équipe EMP002 (attendu du brief)" "200 + EMP004, EMP005, EMP008 (+ EMP009)" `
    "$($r.Status) + $($teamMat -join ', ')" `
    ($r.Status -eq 200 -and $briefTeamOk)

$managerTeamOk = ($teamMat -contains "EMP006" -and $teamMat -contains "EMP007" -and $teamMat -contains "EMP009")
Add-Test "GET équipe EMP002 (cohérence manager_id=2)" "200 + EMP006, EMP007, EMP009" `
    "$($r.Status) + $($teamMat -join ', ')" `
    ($r.Status -eq 200 -and $managerTeamOk)

$r = Invoke-Api -Method "GET" -Path "/api/employees/999"
$fmt = Test-ErrorFormat -Response $r
Add-Test "GET /api/employees/999" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

$r = Invoke-Api -Method "GET" -Path "/api/employees/999/team"
$fmt = Test-ErrorFormat -Response $r
Add-Test "GET /api/employees/999/team" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

$r = Invoke-Api -Method "GET" -Path "/api/employees/abc"
$fmt = Test-ErrorFormat -Response $r
Add-Test "GET /api/employees/abc" "400 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

# ===========================================================================
# 3. CONTRATS
# ===========================================================================
Write-Host "Tests contrats..." -ForegroundColor Cyan

$r = Invoke-Api -Method "GET" -Path "/api/employees/$idEmp005/contracts"
$list = @($r.Json)
$types = @($list | ForEach-Object { "$($_.type) $($_.status)" })
Add-Test "GET contrats EMP005" "200 + 2 contrats (CDD TERMINE + CDI EN_COURS), récent d'abord" `
    "$($r.Status) + $($list.Count) contrat(s) : $($types -join ', ')" `
    ($r.Status -eq 200 -and $list.Count -eq 2 -and $types -contains "CDD TERMINE" -and $types -contains "CDI EN_COURS")

$r = Invoke-Api -Method "GET" -Path "/api/employees/$idEmp004/contracts"
$list = @($r.Json)
$types = @($list | ForEach-Object { "$($_.type) $($_.status)" })
$orderOk = ($list.Count -eq 2 -and [datetime]$list[0].startDate -gt [datetime]$list[1].startDate)
Add-Test "GET contrats EMP004 (contrôle données démo)" "200 + 2 contrats, récent d'abord" `
    "$($r.Status) + $($types -join ', ') + ordreOK=$orderOk" `
    ($r.Status -eq 200 -and $orderOk -and $types -contains "CDD TERMINE" -and $types -contains "CDI EN_COURS")

$r = Invoke-Api -Method "POST" -Path "/api/employees/$idEmp004/contracts" -Body @{
    type = "CDI"; startDate = "2026-11-01"; grossSalary = 12000
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST CDI EMP004 alors qu'un contrat est en cours" "409 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 409 -and $null -eq $fmt)

$cdiEnCours = $null
foreach ($c in $list) { if ($c.status -eq "EN_COURS") { $cdiEnCours = $c } }
if ($cdiEnCours) {
    $r = Invoke-Api -Method "PUT" -Path "/api/contracts/$($cdiEnCours.id)/terminate"
    Add-Test "PUT terminer le CDI en cours d'EMP004" "200 + statut TERMINE + endDate=$today" `
        "$($r.Status) + statut=$($r.Json.status) + endDate=$($r.Json.endDate)" `
        ($r.Status -eq 200 -and $r.Json.status -eq "TERMINE" -and $r.Json.endDate -eq $today)

    $r = Invoke-Api -Method "PUT" -Path "/api/contracts/$($cdiEnCours.id)/terminate"
    $fmt = Test-ErrorFormat -Response $r
    Add-Test "PUT terminer à nouveau le même contrat" "409 + JSON d'erreur conforme" `
        (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
        ($r.Status -eq 409 -and $null -eq $fmt)
} else {
    Add-Test "PUT terminer le CDI en cours d'EMP004" "200 + statut TERMINE + endDate=$today" `
        "Aucun contrat EN_COURS trouvé pour EMP004" $false
    Add-Test "PUT terminer à nouveau le même contrat" "409 + JSON d'erreur conforme" `
        "Test non exécutable (pas de contrat EN_COURS)" $false
}

$r = Invoke-Api -Method "PUT" -Path "/api/contracts/999/terminate"
$fmt = Test-ErrorFormat -Response $r
Add-Test "PUT /api/contracts/999/terminate" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees/$idEmp004/contracts" -Body @{
    type = "CDD"; startDate = "2026-11-01"; grossSalary = 9000
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST CDD sans endDate" "400 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees/$idEmp004/contracts" -Body @{
    type = "CDD"; startDate = "2026-11-01"; endDate = "2026-10-31"; grossSalary = 9000
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST CDD endDate antérieure à startDate" "400 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees/$idEmp004/contracts" -Body @{
    type = "CDX"; startDate = "2026-11-01"; endDate = "2027-10-31"; grossSalary = 9000
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST contrat type CDX" "400 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees/$idEmp004/contracts" -Body @{
    type = "CDD"; startDate = "2026-11-01"; endDate = "2027-10-31"; grossSalary = -5
}
$fmt = Test-ErrorFormat -Response $r -RequireErrors $true
Add-Test "POST contrat grossSalary = -5" "400 + champ errors" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees/$idEmp004/contracts" -Body @{
    type = "CDD"; startDate = "2026-11-01"; endDate = "2027-10-31"
}
$fmt = Test-ErrorFormat -Response $r -RequireErrors $true
Add-Test "POST contrat sans grossSalary" "400 + champ errors" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 400 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees/$idEmp004/contracts" -Body @{
    type = "CDD"; startDate = "2026-11-01"; endDate = "2027-10-31"; grossSalary = 9000
}
$newContractId = if ($r.Json) { $r.Json.id } else { $null }
Add-Test "POST CDD valide pour EMP004" "201 + statut EN_COURS" `
    "$($r.Status) + id=$newContractId + statut=$($r.Json.status)" `
    ($r.Status -eq 201 -and $r.Json.status -eq "EN_COURS" -and $null -ne $newContractId)

$r = Invoke-Api -Method "POST" -Path "/api/employees/$idEmp004/contracts" -Body @{
    type = "CDI"; startDate = "2027-11-01"; grossSalary = 15000
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST deuxième contrat juste après" "409 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 409 -and $null -eq $fmt)

$r = Invoke-Api -Method "POST" -Path "/api/employees/999/contracts" -Body @{
    type = "CDI"; startDate = "2026-11-01"; grossSalary = 12000
}
$fmt = Test-ErrorFormat -Response $r
Add-Test "POST contrat pour employé 999" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

$r = Invoke-Api -Method "GET" -Path "/api/employees/999/contracts"
$fmt = Test-ErrorFormat -Response $r
Add-Test "GET contrats employé 999" "404 + JSON d'erreur conforme" `
    (Format-ErrorObtained -Response $r -FormatIssue $fmt) `
    ($r.Status -eq 404 -and $null -eq $fmt)

# ===========================================================================
# Affichage du tableau
# ===========================================================================
Write-Host ""
Write-Host "================ RESULTATS LIVRABLE 1 ================" -ForegroundColor Yellow
$table = $script:Results |
    Format-Table -AutoSize @(
        @{ Label = "N°"; Expression = { $_.No } },
        @{ Label = "Test"; Expression = { $_.Test } },
        @{ Label = "Attendu"; Expression = { $_.Expected } },
        @{ Label = "Obtenu"; Expression = { $_.Obtained } },
        @{ Label = "OK/KO"; Expression = { $_.OK } }
    ) | Out-String -Width 400
Write-Host $table

$okCount = @($script:Results | Where-Object { $_.OK -eq "OK" }).Count
$koCount = @($script:Results | Where-Object { $_.OK -eq "KO" }).Count
Write-Host "Total : $($script:Results.Count) tests - OK : $okCount - KO : $koCount" -ForegroundColor Yellow
if ($koCount -gt 0) {
    Write-Host "Tests en échec :" -ForegroundColor Red
    foreach ($t in ($script:Results | Where-Object { $_.OK -eq "KO" })) {
        Write-Host ("  [{0}] {1} -> {2}" -f $t.No, $t.Test, $t.Obtained) -ForegroundColor Red
    }
}

# Export Markdown (pratique pour le rapport)
$md = New-Object System.Text.StringBuilder
[void]$md.AppendLine("# Résultats des tests - Livrable 1")
[void]$md.AppendLine("")
[void]$md.AppendLine("Exécution : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') - API : $BaseUrl")
[void]$md.AppendLine("")
[void]$md.AppendLine("| N° | Test | Attendu | Obtenu | OK/KO |")
[void]$md.AppendLine("|---:|---|---|---|:---:|")
foreach ($t in $script:Results) {
    $test = $t.Test.Replace("|", "\|")
    $exp = $t.Expected.Replace("|", "\|")
    $obt = $t.Obtained.Replace("|", "\|")
    [void]$md.AppendLine("| $($t.No) | $test | $exp | $obt | $($t.OK) |")
}
[void]$md.AppendLine("")
[void]$md.AppendLine("Total : $($script:Results.Count) tests - OK : $okCount - KO : $koCount")
$reportPath = Join-Path $PSScriptRoot "resultats-livrable1.md"
Write-JsonFile -Path $reportPath -Content $md.ToString()
Write-Host "Rapport Markdown : $reportPath" -ForegroundColor Cyan

if ($koCount -gt 0) { exit 1 } else { exit 0 }
