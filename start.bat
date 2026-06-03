@echo off
setlocal

echo ========================================
echo   BookTrading Startup Script
echo ========================================
echo.

:: Kill existing processes
echo [1/5] Cleaning up old processes...
taskkill /F /IM java.exe >nul 2>&1
taskkill /F /IM node.exe >nul 2>&1
timeout /t 2 /nobreak >nul

:: Remove old port file
del /Q "backend\target\app.port" >nul 2>&1

:: Start backend
echo [2/5] Starting backend (Spring Boot)...
cd /d "%~dp0backend"
start "BookTrading-Backend" cmd /c "mvnw.cmd spring-boot:run > ..\backend.log 2>&1"

:: Wait for backend port file
echo [3/5] Waiting for backend to start...
set RETRIES=0
:WAIT_PORT
timeout /t 2 /nobreak >nul
set /a RETRIES+=1
if %RETRIES% GTR 30 (
    echo ERROR: Backend failed to start within 60 seconds.
    echo Check backend.log for details.
    goto :END
)
if not exist "target\app.port" goto :WAIT_PORT

set /p BACKEND_PORT=<"target\app.port"
echo   Backend started on port: %BACKEND_PORT%

:: Find a free port for frontend
echo [4/5] Finding free port for frontend...
cd /d "%~dp0frontend"
for /f %%i in ('node find-port.js') do set FRONTEND_PORT=%%i
echo   Frontend port: %FRONTEND_PORT%

:: Start frontend
echo [5/5] Starting frontend (Vue CLI)...
set FRONTEND_PORT=%FRONTEND_PORT%
start "BookTrading-Frontend" cmd /c "set FRONTEND_PORT=%FRONTEND_PORT%&& npm run serve > ..\frontend.log 2>&1"

:: Wait for frontend to start
timeout /t 10 /nobreak >nul

echo.
echo ========================================
echo   BookTrading is running!
echo   Backend:  http://localhost:%BACKEND_PORT%
echo   Frontend: http://localhost:%FRONTEND_PORT%
echo ========================================
echo.
echo Logs:
echo   backend.log  - Backend output
echo   frontend.log - Frontend output
echo.
echo Press any key to stop all processes...
pause >nul

:: Cleanup
echo Stopping processes...
taskkill /F /IM java.exe >nul 2>&1
taskkill /F /IM node.exe >nul 2>&1
echo Done.

:END
endlocal
