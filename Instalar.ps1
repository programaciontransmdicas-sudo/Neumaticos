param([Parameter(Mandatory=$true)][string]$Proyecto)
$ErrorActionPreference = 'Stop'
$destinoProyecto = (Resolve-Path -LiteralPath $Proyecto).Path
if (!(Test-Path -LiteralPath (Join-Path $destinoProyecto 'pom.xml'))) {
    throw 'La carpeta de destino debe ser tu proyecto Maven y contener pom.xml.'
}
if ($destinoProyecto -eq $PSScriptRoot) {
    throw 'Selecciona tu proyecto original como destino, no la carpeta del paquete.'
}
$archivos = @('pom.xml')
Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -File | ForEach-Object {
    $archivos += $_.FullName.Substring($PSScriptRoot.Length + 1)
}
$respaldo = Join-Path $destinoProyecto ('respaldo-neumaticos-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Path $respaldo | Out-Null
$listaNuevos = @()
foreach ($relativo in $archivos) {
    $original = Join-Path $destinoProyecto $relativo
    if (Test-Path -LiteralPath $original) {
        $copia = Join-Path $respaldo $relativo
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $copia) | Out-Null
        Copy-Item -LiteralPath $original -Destination $copia
    } else {
        $listaNuevos += $relativo
    }
}
$listaNuevos | Set-Content -LiteralPath (Join-Path $respaldo 'archivos-nuevos.txt') -Encoding UTF8
foreach ($relativo in $archivos) {
    $origen = Join-Path $PSScriptRoot $relativo
    $destino = Join-Path $destinoProyecto $relativo
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destino) | Out-Null
    Copy-Item -LiteralPath $origen -Destination $destino -Force
}
Write-Host ''
Write-Host 'Archivos instalados correctamente.' -ForegroundColor Green
Write-Host "Respaldo de los archivos reemplazados: $respaldo"
Write-Host 'Tus archivos mvnw.cmd, .mvn y los archivos ajenos al paquete se conservan.'
Write-Host 'La base de datos se prepara al iniciar con tus variables DB_URL, DB_USERNAME y DB_PASSWORD.'
Write-Host 'En la terminal del proyecto ejecuta: .\mvnw.cmd clean spring-boot:run'
