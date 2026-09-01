# Verifies the local toolchain and .env file before boot/build/checkout attempts.
# Usage: .\scripts\check-env.ps1
# Exits 0 if everything required is present, 1 otherwise.

$ErrorActionPreference = 'SilentlyContinue'
$script:fail = $false
$script:warn = $false

function Ok($msg)   { Write-Host "  [OK]   $msg" -ForegroundColor Green }
function Bad($msg)  { Write-Host "  [FAIL] $msg" -ForegroundColor Red; $script:fail = $true }
function Warn($msg) { Write-Host "  [WARN] $msg" -ForegroundColor Yellow; $script:warn = $true }
function Section($title) { Write-Host ""; Write-Host $title }

Section "Java"
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) {
    $verLine = (& java -version 2>&1 | Select-Object -First 1).ToString()
    if ($verLine -match '"(\d+)') {
        $major = [int]$Matches[1]
        if ($major -ge 21) { Ok "java found: $verLine" }
        else { Bad "java found ($verLine) but this project targets Java 25 (21+ should compile; verify explicitly)" }
    } else {
        Warn "java found but version could not be parsed: $verLine"
    }
} else {
    Bad "java not found on PATH. Install a JDK 21+ (Temurin recommended: https://adoptium.net)"
}

Section "Maven Wrapper"
if (Test-Path ".\mvnw.cmd") { Ok ".\mvnw.cmd present" }
else { Bad "mvnw.cmd not found - run this script from the repository root" }

Section "Docker"
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCmd) {
    docker info *> $null
    if ($LASTEXITCODE -eq 0) { Ok "docker daemon is running" }
    else { Bad "docker CLI found but the daemon is not reachable. Start Docker Desktop and retry." }

    docker compose version *> $null
    if ($LASTEXITCODE -eq 0) { Ok "docker compose plugin available" }
    else { Bad "docker compose (v2 plugin) not available. Update Docker Desktop." }
} else {
    Bad "docker not found on PATH. Install Docker Desktop: https://www.docker.com/products/docker-desktop/"
}

Section "OpenSSL (only needed to generate APP_JWT_SECRET)"
$opensslCmd = Get-Command openssl -ErrorAction SilentlyContinue
if (-not $opensslCmd) {
    $defaultPath = "C:\Program Files\OpenSSL-Win64\bin\openssl.exe"
    if (Test-Path $defaultPath) { $opensslCmd = $defaultPath }
}
if ($opensslCmd) {
    $verOut = & $opensslCmd version 2>&1
    Ok "openssl found: $verOut"
} else {
    Warn "openssl not found on PATH. Only needed once, to generate APP_JWT_SECRET."
    Warn "  Install:  winget install -e --id ShiningLight.OpenSSL.Light"
    Warn "  Locate:   winget list --id ShiningLight.OpenSSL.Light"
    Warn "  Then run: & 'C:\Program Files\OpenSSL-Win64\bin\openssl' rand -base64 32"
    Warn "  Or generate the secret any other way: 32+ random bytes, base64-encoded."
}

Section "Port 8080"
$portInUse = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($portInUse) {
    Warn "port 8080 is already in use. Stop whatever is listening, or set SERVER_PORT in .env."
    $portInUse | Select-Object -First 3 | ForEach-Object {
        $proc = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue
        if ($proc) { Warn ("  PID {0} ({1})" -f $_.OwningProcess, $proc.ProcessName) }
    }
} else {
    Ok "port 8080 is free"
}

Section ".env file"
if (Test-Path ".env") {
    Ok ".env exists"
    $nonCommentLines = Get-Content ".env" | Where-Object { $_ -notmatch '^\s*#' }
    if (($nonCommentLines -join "`n") -match '__CHANGE_ME__') {
        Bad ".env still contains __CHANGE_ME__ placeholders - replace them with real local values"
    } else {
        Ok "no __CHANGE_ME__ placeholders left in .env"
    }
    $secretLine = (Get-Content ".env" | Where-Object { $_ -match '^APP_JWT_SECRET=' })
    if ($secretLine) {
        $secretValue = ($secretLine -split '=', 2)[1].Trim()
        if ($secretValue -match 'CHANGE_ME') {
            Bad "APP_JWT_SECRET is still the placeholder value - generate a real one (see below)"
        } elseif ($secretValue.Length -ge 32) {
            Ok "APP_JWT_SECRET looks present and long enough ($($secretValue.Length) chars)"
        } else {
            Bad "APP_JWT_SECRET looks too short ($($secretValue.Length) chars, want 32+)"
        }
    } else {
        Bad "APP_JWT_SECRET not found in .env"
    }
} else {
    Bad ".env not found. Run: Copy-Item .env.example .env   (then fill in the placeholders)"
}

Write-Host ""
if ($script:fail) {
    Write-Host "Some required checks failed - see [FAIL] lines above." -ForegroundColor Red
    exit 1
} elseif ($script:warn) {
    Write-Host "All required checks passed, with warnings above." -ForegroundColor Yellow
    exit 0
} else {
    Write-Host "All checks passed." -ForegroundColor Green
    exit 0
}
