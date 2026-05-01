@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0gui-smoke.ps1" %*
