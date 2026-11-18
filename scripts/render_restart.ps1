$renderCli = "C:\Users\user0\Documents\cloud_project\render_cli\render.exe"
$serviceId = "srv-dbufps0m7kps73c9ddr0"

Write-Host "Restarting Render service '$serviceId'..."
& $renderCli restart $serviceId --confirm
Write-Host "Restart triggered! Follow live logs with:"
Write-Host "  & `"$renderCli`" logs --resources $serviceId --tail"
