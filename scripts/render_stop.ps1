$serviceId = "srv-dbufps0m7kps73c9ddr0"
$config = Get-Content "$env:USERPROFILE\.render\cli.yaml" -Raw

if ($config -match 'key:\s*([^\r\n]+)') {
    $apiKey = $matches[1].Trim()
    $headers = @{ "Authorization" = "Bearer $apiKey" }
    Write-Host "Suspending (stopping) Render service '$serviceId'..."
    try {
        $resp = Invoke-RestMethod -Uri "https://api.render.com/v1/services/$serviceId/suspend" -Method POST -Headers $headers
        Write-Host "Success! The online service has been suspended (stopped)."
        Write-Host "Status: Suspended"
    } catch {
        Write-Host "Failed to suspend service: $_"
    }
} else {
    Write-Host "Could not find Render API key in ~/.render/cli.yaml. Please login first with render login."
}
