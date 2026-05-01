@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-autonomous-loop.ps1" %*
