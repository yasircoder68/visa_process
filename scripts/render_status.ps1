$renderCli = "C:\Users\user0\Documents\cloud_project\render_cli\render.exe"
$serviceId = "srv-dbufps0m7kps73c9ddr0"

Write-Host "=========================================================="
Write-Host "Render Service Status Check"
Write-Host "=========================================================="

# CLI status
& $renderCli deploys list $serviceId --output text

# API status
$config = Get-Content "$env:USERPROFILE\.render\cli.yaml" -Raw
if ($config -match 'key:\s*([^\r\n]+)') {
    $apiKey = $matches[1].Trim()
    $headers = @{ "Authorization" = "Bearer $apiKey" }
    try {
        $res = Invoke-RestMethod -Uri "https://api.render.com/v1/services/$serviceId" -Headers $headers
        $res | ConvertTo-Json -Depth 3
    } catch {
        Write-Host "API Error: $_"
    }
}
