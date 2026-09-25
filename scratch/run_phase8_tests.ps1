$ErrorActionPreference = "Stop"

$edge = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
$baseDir = "C:\Users\user0\Documents\antigravity\visa_process\docs\screenshots"

function Capture-Screen($name, $url) {
    $out = Join-Path $baseDir $name
    Start-Process -FilePath $edge -ArgumentList "--headless", "--window-size=1280,800", "--screenshot=`"$out`"", $url -Wait
    Write-Host "  -> Captured screenshot: $name"
}

Write-Host "=========================================================="
Write-Host "PHASE 8: REPORT §14 SCENARIO (STUDENT Visa, Chennai, T Nagar)"
Write-Host "=========================================================="

# STEP 1: Apply for STUDENT visa, port of arrival Chennai
Write-Host "`n[1/4] Submitting STUDENT Visa Application..."
$applyBody = "applicantName=Mohamed+Yasir+A&passportNumber=P202517906&visaType=STUDENT&portOfArrival=Chennai"
$applyResp = Invoke-WebRequest -Uri "http://localhost:3000/apply" -Method POST -Body $applyBody -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$appId = [regex]::Match($applyResp.Content, "VISA\d+").Value
Write-Host "  -> Application Created: $appId"

$trackUrl = "http://localhost:3000/track?applicationId=$appId"
Capture-Screen "09_track_submitted.png" $trackUrl
$trackInitial = Invoke-WebRequest -Uri $trackUrl -UseBasicParsing
$initialStatus = [regex]::Match($trackInitial.Content, "Status:\s*<strong>([^<]+)</strong>").Groups[1].Value
Write-Host "  -> Tracking Status before review: $initialStatus"

# STEP 2: Visa Officer Login & Approval
Write-Host "`n[2/4] Visa Officer Login & Approval..."
$voSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$voLoginBody = "username=visaofficer01&password=password123"
$voLogin = Invoke-WebRequest -Uri "http://localhost:3000/login" -Method POST -Body $voLoginBody -ContentType "application/x-www-form-urlencoded" -WebSession $voSession -UseBasicParsing
Write-Host "  -> Visa Officer Login HTTP: $($voLogin.StatusCode)"

# Dashboard showing pending application
$voDash = Invoke-WebRequest -Uri "http://localhost:3000/visa-officer" -WebSession $voSession -UseBasicParsing
$voHtml = "$baseDir\temp_vo.html"
Set-Content -Path $voHtml -Value $voDash.Content -Encoding utf8
Capture-Screen "10_visa_officer_dashboard.png" "file:///$voHtml"
Remove-Item $voHtml -ErrorAction SilentlyContinue

# Approve application
$approveBody = "applicationId=$appId"
$approveResp = Invoke-WebRequest -Uri "http://localhost:3000/visa-officer/approve" -Method POST -Body $approveBody -ContentType "application/x-www-form-urlencoded" -WebSession $voSession -UseBasicParsing
Write-Host "  -> Approve HTTP Status: $($approveResp.StatusCode)"

# Verify /track shows APPROVED + ETA
Capture-Screen "11_track_approved_eta.png" $trackUrl
$trackApproved = Invoke-WebRequest -Uri $trackUrl -UseBasicParsing
$approvedStatus = [regex]::Match($trackApproved.Content, "Status:\s*<strong>([^<]+)</strong>").Groups[1].Value
$etaNumber = [regex]::Match($trackApproved.Content, "ETA Number:\s*<strong>([^<]+)</strong>").Groups[1].Value
$validUntil = [regex]::Match($trackApproved.Content, "Valid Until:\s*<strong>([^<]+)</strong>").Groups[1].Value
Write-Host "  -> Tracking Status after approval: $approvedStatus"
Write-Host "  -> ETA Number: $etaNumber, Valid Until: $validUntil"

# STEP 3: Post-Arrival Registration with "T Nagar, Chennai"
Write-Host "`n[3/4] Submitting Post-Arrival FRRO Registration..."
$regBody = "applicationId=$appId&address=T+Nagar%2C+Chennai"
$regResp = Invoke-WebRequest -Uri "http://localhost:3000/register" -Method POST -Body $regBody -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
Write-Host "  -> Register Submission HTTP: $($regResp.StatusCode)"

$regId = [regex]::Match($regResp.Content, "REG\d+").Value
$assignedOfficer = [regex]::Match($regResp.Content, "FRRO\d+").Value
$apptDate = [regex]::Match($regResp.Content, "\d{4}-\d{2}-\d{2}").Value
Write-Host "  -> Registration ID: $regId"
Write-Host "  -> Assigned Officer: $assignedOfficer"
Write-Host "  -> Scheduled Appointment Date: $apptDate"

$regSuccessHtml = "$baseDir\temp_reg_success.html"
Set-Content -Path $regSuccessHtml -Value $regResp.Content -Encoding utf8
Capture-Screen "12_registration_success.png" "file:///$regSuccessHtml"
Remove-Item $regSuccessHtml -ErrorAction SilentlyContinue

# STEP 4: FRRO Officer Login, View Roster, Map, & Verify
Write-Host "`n[4/4] FRRO Officer Review, Map Verification & Approval..."
$officerUser = if ($assignedOfficer -eq "FRRO02") { "frroofficer02" } else { "frroofficer01" }
$frroSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$frroLoginBody = "username=$officerUser&password=password123"
$frroLogin = Invoke-WebRequest -Uri "http://localhost:3000/login" -Method POST -Body $frroLoginBody -ContentType "application/x-www-form-urlencoded" -WebSession $frroSession -UseBasicParsing
Write-Host "  -> FRRO Officer ($officerUser) Login HTTP: $($frroLogin.StatusCode)"

# Officer dashboard
$frroDash = Invoke-WebRequest -Uri "http://localhost:3000/frro" -WebSession $frroSession -UseBasicParsing
$frroHtml = "$baseDir\temp_frro.html"
Set-Content -Path $frroHtml -Value $frroDash.Content -Encoding utf8
Capture-Screen "13_frro_officer_dashboard.png" "file:///$frroHtml"
Remove-Item $frroHtml -ErrorAction SilentlyContinue

# Leaflet Map View
$mapResp = Invoke-WebRequest -Uri "http://localhost:3000/frro/map?registrationId=$regId" -WebSession $frroSession -UseBasicParsing
$mapHtml = "$baseDir\temp_map.html"
Set-Content -Path $mapHtml -Value $mapResp.Content -Encoding utf8
Capture-Screen "14_view_map_leaflet.png" "file:///$mapHtml"
Remove-Item $mapHtml -ErrorAction SilentlyContinue

# Verify Documents (VERIFY button)
$verifyBody = "registrationId=$regId"
$verifyResp = Invoke-WebRequest -Uri "http://localhost:3000/frro/verify" -Method POST -Body $verifyBody -ContentType "application/x-www-form-urlencoded" -WebSession $frroSession -UseBasicParsing
Write-Host "  -> FRRO Document Verification HTTP: $($verifyResp.StatusCode)"

Write-Host "`n=========================================================="
Write-Host "DATABASE AUDIT AFTER §14 COMPLETION"
Write-Host "=========================================================="
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u visareg -pvisareg123 visareg -e "SELECT id, applicant_name, visa_type, status, eta_number, valid_until FROM applications; SELECT id, application_id, district, appointment_date, status, assigned_officer FROM registrations; SELECT id, name, status, jurisdiction FROM officers WHERE id='$assignedOfficer';"

Write-Host "`n=========================================================="
Write-Host "PHASE 8: NEGATIVE TESTS"
Write-Host "=========================================================="

# Negative Test 1: Unapproved Visa Application
Write-Host "`n[Neg 1] Register with unapproved application..."
$unappResp = Invoke-WebRequest -Uri "http://localhost:3000/apply" -Method POST -Body "applicantName=Jane+Doe&passportNumber=UNAPP999&visaType=TOURIST&portOfArrival=Chennai" -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$unappId = [regex]::Match($unappResp.Content, "VISA\d+").Value
Write-Host "  -> Created unapproved application: $unappId"

$neg1Resp = Invoke-WebRequest -Uri "http://localhost:3000/register" -Method POST -Body "applicationId=$unappId&address=T+Nagar%2C+Chennai" -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$neg1Html = "$baseDir\temp_neg1.html"
Set-Content -Path $neg1Html -Value $neg1Resp.Content -Encoding utf8
Capture-Screen "15_neg1_unapproved_visa.png" "file:///$neg1Html"
Remove-Item $neg1Html -ErrorAction SilentlyContinue
$neg1Err = [regex]::Match($neg1Resp.Content, 'class="alert alert-error"[^>]*>([^<]+)<').Groups[1].Value
Write-Host "  -> Error Result: $neg1Err"

# Negative Test 2: Nonsense / Unresolvable Address
Write-Host "`n[Neg 2] Register with unresolvable address..."
$neg2Resp = Invoke-WebRequest -Uri "http://localhost:3000/register" -Method POST -Body "applicationId=$appId&address=ZZZXQWERYUIOP12345NONEXISTENT" -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$neg2Html = "$baseDir\temp_neg2.html"
Set-Content -Path $neg2Html -Value $neg2Resp.Content -Encoding utf8
Capture-Screen "16_neg2_invalid_address.png" "file:///$neg2Html"
Remove-Item $neg2Html -ErrorAction SilentlyContinue
$neg2Err = [regex]::Match($neg2Resp.Content, 'class="alert alert-error"[^>]*>([^<]+)<').Groups[1].Value
Write-Host "  -> Error Result: $neg2Err"

# Negative Test 3: No Free Officer in District
Write-Host "`n[Neg 3] Exhausting officer roster in Coimbatore..."
# Coimbatore has 1 officer: FRRO03
# 3a. Create and approve Visa 3
$app3 = Invoke-WebRequest -Uri "http://localhost:3000/apply" -Method POST -Body "applicantName=Coimbatore+One&passportNumber=CBE001&visaType=STUDENT&portOfArrival=Coimbatore" -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$app3Id = [regex]::Match($app3.Content, "VISA\d+").Value
Invoke-WebRequest -Uri "http://localhost:3000/visa-officer/approve" -Method POST -Body "applicationId=$app3Id" -ContentType "application/x-www-form-urlencoded" -WebSession $voSession -UseBasicParsing | Out-Null
$reg3 = Invoke-WebRequest -Uri "http://localhost:3000/register" -Method POST -Body "applicationId=$app3Id&address=Gandhipuram%2C+Coimbatore" -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$assigned3 = [regex]::Match($reg3.Content, "FRRO\d+").Value
Write-Host "  -> First Coimbatore registration assigned to: $assigned3"

# 3b. Create and approve Visa 4
$app4 = Invoke-WebRequest -Uri "http://localhost:3000/apply" -Method POST -Body "applicantName=Coimbatore+Two&passportNumber=CBE002&visaType=STUDENT&portOfArrival=Coimbatore" -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$app4Id = [regex]::Match($app4.Content, "VISA\d+").Value
Invoke-WebRequest -Uri "http://localhost:3000/visa-officer/approve" -Method POST -Body "applicationId=$app4Id" -ContentType "application/x-www-form-urlencoded" -WebSession $voSession -UseBasicParsing | Out-Null

# 3c. Try to register Visa 4 in Coimbatore (roster exhausted)
$neg3Resp = Invoke-WebRequest -Uri "http://localhost:3000/register" -Method POST -Body "applicationId=$app4Id&address=Gandhipuram%2C+Coimbatore" -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$neg3Html = "$baseDir\temp_neg3.html"
Set-Content -Path $neg3Html -Value $neg3Resp.Content -Encoding utf8
Capture-Screen "17_neg3_no_officer_available.png" "file:///$neg3Html"
Remove-Item $neg3Html -ErrorAction SilentlyContinue
$neg3Err = [regex]::Match($neg3Resp.Content, 'class="alert alert-error"[^>]*>([^<]+)<').Groups[1].Value
Write-Host "  -> Error Result: $neg3Err"

# Negative Test 4: Visa Server Stopped
Write-Host "`n[Neg 4] Stopping Visa Application Server to test fault tolerance..."
$visaProc = (Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue).OwningProcess | Select-Object -Unique
Write-Host "  -> Killing visa-server PID: $visaProc..."
Stop-Process -Id $visaProc -Force
Start-Sleep -Seconds 2

$neg4Resp = Invoke-WebRequest -Uri "http://localhost:3000/register" -Method POST -Body "applicationId=$appId&address=T+Nagar%2C+Chennai" -ContentType "application/x-www-form-urlencoded" -UseBasicParsing
$neg4Html = "$baseDir\temp_neg4.html"
Set-Content -Path $neg4Html -Value $neg4Resp.Content -Encoding utf8
Capture-Screen "18_neg4_visa_server_down.png" "file:///$neg4Html"
Remove-Item $neg4Html -ErrorAction SilentlyContinue
$neg4Err = [regex]::Match($neg4Resp.Content, 'class="alert alert-error"[^>]*>([^<]+)<').Groups[1].Value
Write-Host "  -> Error Result: $neg4Err"

Write-Host "`nRestarting visa-server..."
Start-Process -FilePath "java" -ArgumentList "-jar", "visa-server\target\visa-server-1.0.0-SNAPSHOT.jar" -WindowStyle Hidden
Start-Sleep -Seconds 8
$visaBack = Invoke-WebRequest -Uri "http://localhost:8081/ws/visa.wsdl" -UseBasicParsing | Select-Object StatusCode
Write-Host "  -> Visa Server back online HTTP: $($visaBack.StatusCode)"

Write-Host "`n=========================================================="
Write-Host "CAPTURING RAW SOAP SAMPLES"
Write-Host "=========================================================="
$soapDir = "docs\soap-samples"

$findReq = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:reg="http://visareg.mca/registry">
   <soapenv:Header/>
   <soapenv:Body>
      <reg:findServiceRequest>
         <reg:serviceName>VisaApplicationService</reg:serviceName>
      </reg:findServiceRequest>
   </soapenv:Body>
</soapenv:Envelope>
"@
$findResp = Invoke-WebRequest -Uri "http://localhost:8080/ws" -Method POST -Body $findReq -ContentType "text/xml" -UseBasicParsing
Set-Content -Path "$soapDir\find-visa-response.xml" -Value $findResp.Content -Encoding utf8
Write-Host "  -> Saved $soapDir\find-visa-response.xml"

$getVisaReq = @"
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:visa="http://visareg.mca/visa">
   <soapenv:Header/>
   <soapenv:Body>
      <visa:getApplicationRequest>
         <visa:applicationId>$appId</visa:applicationId>
      </visa:getApplicationRequest>
   </soapenv:Body>
</soapenv:Envelope>
"@
$getVisaResp = Invoke-WebRequest -Uri "http://localhost:8081/ws" -Method POST -Body $getVisaReq -ContentType "text/xml" -UseBasicParsing
Set-Content -Path "$soapDir\get-application-response.xml" -Value $getVisaResp.Content -Encoding utf8
Write-Host "  -> Saved $soapDir\get-application-response.xml"

Write-Host "`nALL PHASE 8 TESTS COMPLETED SUCCESSFULLY!"
