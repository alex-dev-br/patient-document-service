# Para executar:    . .\scripts\load-env.ps1
# Também pode ser usado com outro arquivo. Exemplo:     . .\scripts\load-env.ps1 -EnvFile ".env.local"

param(
    [string]$EnvFile = ".env"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path $EnvFile)) {
    throw "Arquivo de variáveis não encontrado: $EnvFile"
}

$loadedVariables = 0

Get-Content $EnvFile |
    Where-Object {
        $_ -match '^[^#\s][^=]*='
    } |
    ForEach-Object {
        $key, $value = $_ -split '=', 2

        $key = $key.Trim()
        $value = $value.Trim()

        if (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [Environment]::SetEnvironmentVariable(
            $key,
            $value,
            [EnvironmentVariableTarget]::Process
        )

        $loadedVariables++
    }

Write-Host "$loadedVariables variáveis carregadas de '$EnvFile' para o processo atual."