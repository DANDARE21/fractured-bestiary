# Run Multiplayer Environment for Fractured Utils Testing
# Launches dedicated server, OP client (Player1), and Non-OP client (Player2)

Write-Host "=============================================" -ForegroundColor Gold
Write-Host " Launching Fractured Utils Multiplayer Test  " -ForegroundColor Gold
Write-Host "=============================================" -ForegroundColor Gold

Write-Host "`n[1/3] Starting Dedicated Server..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot'; Write-Host '--- MINECRAFT SERVER ---' -ForegroundColor Green; .\gradlew.bat runServer"

Write-Host "Waiting 10 seconds for server to initialize..." -ForegroundColor Gray
Start-Sleep -Seconds 10

Write-Host "[2/3] Starting Client #1 (Player1 - OP)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot'; Write-Host '--- CLIENT 1: Player1 (OP) ---' -ForegroundColor Cyan; .\gradlew.bat runClient --args='--username Player1'"

Write-Host "[3/3] Starting Client #2 (Player2 - Non-OP)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "Set-Location '$PSScriptRoot'; Write-Host '--- CLIENT 2: Player2 (Non-OP) ---' -ForegroundColor Yellow; .\gradlew.bat runClient --args='--username Player2'"

Write-Host "`nAll 3 instances launched in separate windows!" -ForegroundColor Green
