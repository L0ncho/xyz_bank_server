# Generates a development PKCS12 keystore for BFF HTTPS (self-signed, localhost).
# Requires keytool from a JDK on PATH.

param(
    [string]$Output = (Join-Path $PSScriptRoot "..\platform\shared-security\src\main\resources\keystore.p12"),
    [string]$Password = "changeit",
    [string]$Alias = "xyzbank"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path (Split-Path $Output) | Out-Null
if (Test-Path $Output) {
    Remove-Item -Force $Output
}

keytool -genkeypair `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -storetype PKCS12 `
    -keystore $Output `
    -validity 3650 `
    -storepass $Password `
    -keypass $Password `
    -dname "CN=localhost,OU=XYZBank,O=Duoc,C=CL" `
    -noprompt

Write-Host "Wrote $Output"
