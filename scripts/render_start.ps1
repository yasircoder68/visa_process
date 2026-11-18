$serviceId = "srv-dbufps0m7kps73c9ddr0"
$config = Get-Content "$env:USERPROFILE\.render\cli.yaml" -Raw

if ($config -match 'key:\s*([^\r\n]+)') {
    $apiKey = $matches[1].Trim()
    $headers = @{ "Authorization" = "Bearer $apiKey" }
    Write-Host "Resuming (starting) Render service '$serviceId'..."
    try {
        $resp = Invoke-RestMethod -Uri "https://api.render.com/v1/services/$serviceId/resume" -Method POST -Headers $headers
        Write-Host "Success! The online service has been resumed (started)."
        Write-Host "Public URL: https://visa-registration-system.onrender.com"
    } catch {
        Write-Host "Failed to resume service: $_"
    }
} else {
    Write-Host "Could not find Render API key in ~/.render/cli.yaml. Please login first with render login."
}
