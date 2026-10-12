param(
    [string]$ModularUiPath = (Join-Path $PSScriptRoot '../../../other_mods/ModularUI'),
    [int]$Port = 8765,
    [switch]$NoBrowser
)
$ErrorActionPreference = 'Stop'
$previewScript = Join-Path $ModularUiPath 'tools/xml-preview/server.py'
if (-not (Test-Path -LiteralPath $previewScript)) {
    throw "找不到预览工具：$previewScript；请通过 -ModularUiPath 指定 ModularUI 仓库。"
}
$resourceRoot = Join-Path $PSScriptRoot 'src/main/resources/assets/neofontrender_mui_xml_showcase/mui'
$previewArgs = @($previewScript, $resourceRoot, '--port', $Port)
if ($NoBrowser) { $previewArgs += '--no-browser' }
& python @previewArgs
exit $LASTEXITCODE
