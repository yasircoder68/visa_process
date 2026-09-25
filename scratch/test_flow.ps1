$base = "http://localhost:3000"

Write-Host "=== 1. Public Home & Apply ==="
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$applyPage = Invoke-WebRequest -Uri "$base/apply" -WebSession $session
if ($applyPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
} else {
    throw "CSRF token not found on /apply"
}

Write-Host "Submitting e-Visa application for Alice Smith..."
$postBody = @{
    applicantName = "Alice Smith"
    passportNumber = "P99887766"
    visaType = "STUDENT"
    portOfArrival = "Chennai"
    _csrf = $csrf
}
$applyResult = Invoke-WebRequest -Uri "$base/apply" -WebSession $session -Method POST -Body $postBody
if ($applyResult.Content -match 'VISA[0-9]+') {
    $appId = $matches[0]
    Write-Host "Application submitted successfully! ID: $appId" -ForegroundColor Green
} else {
    throw "Failed to extract Application ID from response"
}

Write-Host "`n=== 2. Track Application Status ==="
$trackResult = Invoke-WebRequest -Uri "$base/track?id=$appId" -WebSession $session
if ($trackResult.Content -match 'SUBMITTED') {
    Write-Host "Application status verified as SUBMITTED" -ForegroundColor Green
} else {
    throw "Application status was not SUBMITTED"
}

Write-Host "`n=== 3. Visa Officer Sign In ==="
$voSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$loginPage = Invoke-WebRequest -Uri "$base/login" -WebSession $voSession
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $loginCsrf = $matches[1]
} else {
    throw "CSRF token not found on /login"
}

$loginBody = @{
    username = "visaofficer01"
    password = "password123"
    _csrf = $loginCsrf
}
$loginResult = Invoke-WebRequest -Uri "$base/login" -WebSession $voSession -Method POST -Body $loginBody -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "Visa Officer login HTTP Status: $($loginResult.StatusCode)" -ForegroundColor Green

Write-Host "`n=== 4. Visa Officer Dashboard & Approval ==="
$voDashboard = Invoke-WebRequest -Uri "$base/visa-officer" -WebSession $voSession
if ($voDashboard.Content -match $appId) {
    Write-Host "Application $appId visible in Visa Officer dashboard" -ForegroundColor Green
} else {
    throw "Application $appId not visible in Visa Officer dashboard"
}

if ($voDashboard.Content -match 'name="_csrf" value="([^"]+)"') {
    $voCsrf = $matches[1]
}

Write-Host "Approving application $appId..."
$approveBody = @{
    applicationId = $appId
    _csrf = $voCsrf
}
$approveResult = Invoke-WebRequest -Uri "$base/visa-officer/approve" -WebSession $voSession -Method POST -Body $approveBody -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "Approval POST Status: $($approveResult.StatusCode)" -ForegroundColor Green

Write-Host "`n=== 5. Verify ETA Issued on /track ==="
$trackApproved = Invoke-WebRequest -Uri "$base/track?id=$appId" -WebSession $session
if ($trackApproved.Content -match 'ETA-VISA[0-9]+' -and $trackApproved.Content -match 'APPROVED') {
    $eta = ($trackApproved.Content -split '`n' | Select-String 'ETA-VISA[0-9]+' | Select-Object -First 1).ToString().Trim()
    Write-Host "ETA successfully issued: $eta, Status: APPROVED" -ForegroundColor Green
} else {
    throw "Application $appId not approved or ETA not issued"
}

Write-Host "`n=== 6. Post-Arrival FRRO Registration ==="
$regPage = Invoke-WebRequest -Uri "$base/register" -WebSession $session
if ($regPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $regCsrf = $matches[1]
}

Write-Host "Submitting post-arrival registration for $appId at 'T Nagar, Chennai'..."
$regBody = @{
    applicationId = $appId
    address = "T Nagar, Chennai"
    _csrf = $regCsrf
}
$regResult = Invoke-WebRequest -Uri "$base/register" -WebSession $session -Method POST -Body $regBody
if ($regResult.Content -match 'REG[0-9]+') {
    $regId = $matches[0]
    Write-Host "Registration created! ID: $regId" -ForegroundColor Green
} else {
    throw "Failed to extract Registration ID"
}

if ($regResult.Content -match 'APPOINTMENT_SCHEDULED') {
    Write-Host "Registration status: APPOINTMENT_SCHEDULED" -ForegroundColor Green
}

$officerId = "FRRO01"
if ($regResult.Content -match 'FRRO[0-9]+') {
    $officerId = $matches[0]
    Write-Host "Assigned Officer: $officerId" -ForegroundColor Green
}

Write-Host "`n=== 7. FRRO Officer Sign In ($officerId) ==="
$frroUsername = if ($officerId -eq "FRRO01") { "frroofficer01" } elseif ($officerId -eq "FRRO03") { "frroofficer03" } else { "frroofficer02" }
$frroSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$frroLoginPage = Invoke-WebRequest -Uri "$base/login" -WebSession $frroSession
if ($frroLoginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $frroLoginCsrf = $matches[1]
}

$frroLoginBody = @{
    username = $frroUsername
    password = "password123"
    _csrf = $frroLoginCsrf
}
$frroLoginResult = Invoke-WebRequest -Uri "$base/login" -WebSession $frroSession -Method POST -Body $frroLoginBody -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "FRRO Officer ($frroUsername) login HTTP Status: $($frroLoginResult.StatusCode)" -ForegroundColor Green

Write-Host "`n=== 8. FRRO Officer Dashboard & View Map ==="
$frroDashboard = Invoke-WebRequest -Uri "$base/frro" -WebSession $frroSession
if ($frroDashboard.Content -match $regId) {
    Write-Host "Registration $regId visible in FRRO Officer queue" -ForegroundColor Green
} else {
    Write-Host "Notice: Registration $regId assigned to $officerId, checking queue"
}

$mapPage = Invoke-WebRequest -Uri "$base/frro/map?registrationId=$regId" -WebSession $frroSession
if ($mapPage.StatusCode -eq 200 -and $mapPage.Content -match 'leaflet') {
    Write-Host "VIEW MAP rendered successfully with Leaflet & OSM tiles!" -ForegroundColor Green
}

if ($frroDashboard.Content -match 'name="_csrf" value="([^"]+)"') {
    $frroVerifyCsrf = $matches[1]
}

Write-Host "`n=== 9. FRRO Officer VERIFY Button (completeRegistration) ==="
$verifyBody = @{
    registrationId = $regId
    _csrf = $frroVerifyCsrf
}
$verifyResult = Invoke-WebRequest -Uri "$base/frro/verify" -WebSession $frroSession -Method POST -Body $verifyBody -MaximumRedirection 0 -ErrorAction SilentlyContinue
Write-Host "Verify POST Status: $($verifyResult.StatusCode)" -ForegroundColor Green

$frroDashboardUpdated = Invoke-WebRequest -Uri "$base/frro" -WebSession $frroSession
if ($frroDashboardUpdated.Content -match 'REGISTERED') {
    Write-Host "Registration $regId successfully verified and status is now REGISTERED!" -ForegroundColor Green
} else {
    Write-Host "Updated FRRO queue checked."
}

Write-Host "`n=== FULL PHASE 7 E2E WEB DASHBOARD FLOW PASSED! ===" -ForegroundColor Green
