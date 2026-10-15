# Genera il "graphic caratteristico" 1024x500 per la scheda Google Play.
# Stesso stile dell'icona: fondo scuro in gradiente, tre barre in
# blu, titolo a sinistra. Il testo e in primo piano perche Play non
# applica maschere a questa immagine.
Add-Type -AssemblyName System.Drawing

$W = 1024
$H = 500
# Percorso relativo a questo script, cosi funziona su qualsiasi macchina.
$storeDir = Join-Path (Split-Path $PSScriptRoot -Parent) "store"
$out = Join-Path $storeDir "feature-graphic-1024x500.png"

$bmp = New-Object System.Drawing.Bitmap($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

# --- fondo: stesso gradiente dell'icona, ma in orizzontale ---
$grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point($W, $H)),
    [System.Drawing.Color]::FromArgb(255, 0x1E, 0x27, 0x39),
    [System.Drawing.Color]::FromArgb(255, 0x09, 0x0C, 0x12))
$g.FillRectangle($grad, 0, 0, $W, $H)

# --- tre barre a destra, come nell'icona ma piu grandi ---
$bars = @(
    @{ x = 690; w = 62; top = 250; topR = 0x3F; topG = 0xA9; topB = 0xDC; botR = 0x14; botG = 0x60; botB = 0x8E },
    @{ x = 778; w = 62; top = 180; topR = 0x4F; topG = 0xC3; topB = 0xF7; botR = 0x1A; botG = 0x74; botB = 0xA9 },
    @{ x = 866; w = 62; top = 100; topR = 0x74; topG = 0xDE; topB = 0xFF; botR = 0x22; botG = 0x89; botB = 0xC8 }
)
$bottom = 400
$r = 31

foreach ($b in $bars) {
    $x0 = [single]$b.x; $x1 = [single]($b.x + $b.w)
    $y0 = [single]$b.top; $y1 = [single]$bottom
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
    $bg.Dispose(); $p.Dispose()
}

# --- testo a sinistra ---
function Font($s, $b) { New-Object System.Drawing.Font("Segoe UI", $s, $b) }

$fTitle = Font 62 ([System.Drawing.FontStyle]::Bold)
$fSub = Font 26 ([System.Drawing.FontStyle]::Regular)
$white = [System.Drawing.Brushes]::White
$muted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 0x8A, 0x8A, 0xA0))

$g.DrawString("UV Profiles", $fTitle, $white, (New-Object System.Drawing.PointF(80, 175)))
$g.DrawString("Profili CPU/GPU per Exynos 990", $fSub, $muted, (New-Object System.Drawing.PointF(80, 262)))
$g.DrawString("Eco  -  Balanced  -  Sixcore  -  Sport", $fSub, $muted, (New-Object System.Drawing.PointF(80, 310)))

$fTitle.Dispose(); $fSub.Dispose(); $muted.Dispose()
$grad.Dispose(); $g.Dispose()

$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Output "scritto: $out"