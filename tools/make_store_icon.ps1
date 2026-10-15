# Genera l'icona 512x512 per la scheda Google Play, con lo stesso
# disegno dell'icona adattiva (tre barre a capsula, altezza crescente)
# ma con la geometria allargata per riempire meglio il formato pieno,
# che Play non ritaglia con una maschera.
Add-Type -AssemblyName System.Drawing

# Percorso relativo a questo script, cosi funziona su qualsiasi macchina.
$storeDir = Join-Path (Split-Path $PSScriptRoot -Parent) "store"
$out = Join-Path $storeDir "icon-512.png"

$S = 512
$u = $S / 108.0

$bmp = New-Object System.Drawing.Bitmap($S, $S, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

# --- fondo: gradiente diagonale scuro, come ic_launcher_background ---
$grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point($S, $S)),
    [System.Drawing.Color]::FromArgb(255, 0x1E, 0x27, 0x39),
    [System.Drawing.Color]::FromArgb(255, 0x0A, 0x0D, 0x14))
$g.FillRectangle($grad, 0, 0, $S, $S)

# --- tre barre: x, larghezza, top (in unita viewport 108),
#     colore alto e basso esplicitati in RGB ---
$bars = @(
    @{ x = 21; w = 18; top = 50; topR = 0x3F; topG = 0xA9; topB = 0xDC; botR = 0x14; botG = 0x60; botB = 0x8E },
    @{ x = 45; w = 18; top = 36; topR = 0x4F; topG = 0xC3; topB = 0xF7; botR = 0x1A; botG = 0x74; botB = 0xA9 },
    @{ x = 69; w = 18; top = 22; topR = 0x74; topG = 0xDE; topB = 0xFF; botR = 0x22; botG = 0x89; botB = 0xC8 }
)

$bottom = 86
$r = 9 * $u   # raggio = meta larghezza: barra a capsula

foreach ($b in $bars) {
    $x0 = $b.x * $u
    $x1 = ($b.x + $b.w) * $u
    $y0 = $b.top * $u
    $y1 = $bottom * $u

    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $p.AddArc($x0, $y0, $d, $d, 180, 90)
    $p.AddArc($x1 - $d, $y0, $d, $d, 270, 90)
    $p.AddArc($x1 - $d, $y1 - $d, $d, $d, 0, 90)
    $p.AddArc($x0, $y1 - $d, $d, $d, 90, 90)
    $p.CloseFigure()

    $bg = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.PointF(0, $y0)),
        (New-Object System.Drawing.PointF(0, $y1)),
        [System.Drawing.Color]::FromArgb(255, $b.topR, $b.topG, $b.topB),
        [System.Drawing.Color]::FromArgb(255, $b.botR, $b.botG, $b.botB))
    $g.FillPath($bg, $p)
    $bg.Dispose()
    $p.Dispose()
}

$grad.Dispose()
$g.Dispose()

$out = "C:\Intel\note20\uv-widget\store\icon-512.png"
[System.IO.Directory]::CreateDirectory($storeDir) | Out-Null
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "scritto: $out"