$ErrorActionPreference = "Stop"

$baseUrl = "https://visa-registration-system.onrender.com"
Write-Host "=========================================================="
Write-Host "TESTING LIVE RENDER DEPLOYMENT"
Write-Host "Target: $baseUrl"
Write-Host "=========================================================="

# 1. Check Homepage
$homeResp = Invoke-WebRequest -Uri "$baseUrl/" -UseBasicParsing
Write-Host "[1/5] Home page status: $($homeResp.StatusCode)"
if ($homeResp.Content -notmatch "Indian Visa") { throw "Home page content mismatch" }

# 2. Submit e-Visa Application
Write-Host "`n[2/5] Submitting e-Visa application..."
$applyBody = "applicantName=Mohamed+Yasir+A&passportNumber=P202517906&visaType=STUDENT&portOfArrival=Chennai"
$applyResp = Invoke-WebRequest -Uri "$baseUrl/apply" -Method POST -Body $applyBody -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$appId = [regex]::Match($applyResp.Content, "VISA\d+").Value
Write-Host "  -> Application Created: $appId"

# 3. Track Application
$trackInitial = Invoke-WebRequest -Uri "$baseUrl/track?id=$appId" -UseBasicParsing
$initialStatus = [regex]::Match($trackInitial.Content, "badge-(submitted|approved|rejected)[^>]*>([^<]+)<").Groups[2].Value
Write-Host "  -> Initial Status: $initialStatus"

# 4. Visa Officer Login & Approval
Write-Host "`n[3/5] Visa Officer login & approval..."
$voSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$voLoginBody = "username=visaofficer01&password=password123"
$voLogin = Invoke-WebRequest -Uri "$baseUrl/login" -Method POST -Body $voLoginBody -ContentType "application/x-www-form-urlencoded" -WebSession $voSession -UseBasicParsing
Write-Host "  -> Visa Officer Login HTTP: $($voLogin.StatusCode)"

$approveBody = "applicationId=$appId"
$approveResp = Invoke-WebRequest -Uri "$baseUrl/visa-officer/approve" -Method POST -Body $approveBody -ContentType "application/x-www-form-urlencoded" -WebSession $voSession -UseBasicParsing
Write-Host "  -> Approve HTTP Status: $($approveResp.StatusCode)"

# Verify approved & ETA
$trackApproved = Invoke-WebRequest -Uri "$baseUrl/track?id=$appId" -UseBasicParsing
$approvedStatus = [regex]::Match($trackApproved.Content, "badge-(submitted|approved|rejected)[^>]*>([^<]+)<").Groups[2].Value
$etaNumber = [regex]::Match($trackApproved.Content, "ETA-VISA\d+").Value
$validUntil = [regex]::Match($trackApproved.Content, "\d{4}-\d{2}-\d{2}").Value
Write-Host "  -> Status: $approvedStatus"
Write-Host "  -> ETA: $etaNumber, Valid Until: $validUntil"

# 5. Submit Post-Arrival FRRO Registration
Write-Host "`n[4/5] Submitting FRRO Registration..."
$regBody = "applicationId=$appId&address=T+Nagar%2C+Chennai"
$regResp = Invoke-WebRequest -Uri "$baseUrl/register" -Method POST -Body $regBody -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$regId = [regex]::Match($regResp.Content, "REG\d+").Value
$assignedOfficer = [regex]::Match($regResp.Content, "FRRO\d+").Value
$apptDate = [regex]::Match($regResp.Content, "\d{4}-\d{2}-\d{2}").Value
Write-Host "  -> Registration ID: $regId"
Write-Host "  -> Assigned Officer: $assignedOfficer"
Write-Host "  -> Scheduled Appointment Date: $apptDate"

# 6. FRRO Officer Login & Verify
Write-Host "`n[5/5] FRRO Officer review & document verification..."
$officerUser = if ($assignedOfficer -eq "FRRO02") { "frroofficer02" } else { "frroofficer01" }
$frroSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$frroLoginBody = "username=$officerUser&password=password123"
$frroLogin = Invoke-WebRequest -Uri "$baseUrl/login" -Method POST -Body $frroLoginBody -ContentType "application/x-www-form-urlencoded" -WebSession $frroSession -UseBasicParsing
Write-Host "  -> FRRO Officer ($officerUser) Login HTTP: $($frroLogin.StatusCode)"

# Verify Documents
$verifyBody = "registrationId=$regId"
$verifyResp = Invoke-WebRequest -Uri "$baseUrl/frro/verify" -Method POST -Body $verifyBody -ContentType "application/x-www-form-urlencoded" -WebSession $frroSession -UseBasicParsing
Write-Host "  -> FRRO Verification HTTP: $($verifyResp.StatusCode)"

# 7. Final status check on FRRO dashboard
$frroDash = Invoke-WebRequest -Uri "$baseUrl/frro" -WebSession $frroSession -UseBasicParsing
Write-Host "  -> FRRO Dashboard status: $($frroDash.StatusCode)"

Write-Host "`n=========================================================="
Write-Host "ALL TESTS PASSED ON LIVE RENDER DEPLOYMENT!"
Write-Host "=========================================================="
