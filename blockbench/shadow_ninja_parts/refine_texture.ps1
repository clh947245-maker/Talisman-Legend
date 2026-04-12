$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$geoPath = Join-Path $root "shadow_ninja_full.geo.json"
$textureDir = Join-Path $root "textures"
$basePath = Join-Path $textureDir "shadow_ninja_texture_base.png"
$outPath = Join-Path $textureDir "shadow_ninja_texture_refined.png"

$geo = Get-Content $geoPath -Raw | ConvertFrom-Json
$geometry = $geo.'minecraft:geometry'[0]

function Adjust-Color {
    param(
        [System.Drawing.Color]$Color,
        [int]$R,
        [int]$G,
        [int]$B
    )

    $nr = [Math]::Max(0, [Math]::Min(255, $Color.R + $R))
    $ng = [Math]::Max(0, [Math]::Min(255, $Color.G + $G))
    $nb = [Math]::Max(0, [Math]::Min(255, $Color.B + $B))
    return [System.Drawing.Color]::FromArgb($Color.A, $nr, $ng, $nb)
}

function Get-BoneBaseColor {
    param([string]$BoneName)

    switch -Regex ($BoneName) {
        "eye" { return [System.Drawing.Color]::FromArgb(255, 220, 35, 35) }
        "visor" { return [System.Drawing.Color]::FromArgb(255, 72, 164, 178) }
        "head" { return [System.Drawing.Color]::FromArgb(255, 16, 16, 20) }
        "belt|loin" { return [System.Drawing.Color]::FromArgb(255, 111, 40, 47) }
        "hand|shin|foot" { return [System.Drawing.Color]::FromArgb(255, 88, 131, 138) }
        "shoulder|overlay" { return [System.Drawing.Color]::FromArgb(255, 43, 43, 48) }
        default { return [System.Drawing.Color]::FromArgb(255, 22, 22, 26) }
    }
}

function Get-FaceRects {
    param(
        [int]$U,
        [int]$V,
        [int]$X,
        [int]$Y,
        [int]$Z
    )

    return @(
        @{ Face = "left"; Rect = [System.Drawing.Rectangle]::new($U, $V + $Z, $Z, $Y) }
        @{ Face = "front"; Rect = [System.Drawing.Rectangle]::new($U + $Z, $V + $Z, $X, $Y) }
        @{ Face = "right"; Rect = [System.Drawing.Rectangle]::new($U + $Z + $X, $V + $Z, $Z, $Y) }
        @{ Face = "back"; Rect = [System.Drawing.Rectangle]::new($U + $Z + $X + $Z, $V + $Z, $X, $Y) }
        @{ Face = "top"; Rect = [System.Drawing.Rectangle]::new($U + $Z, $V, $X, $Z) }
        @{ Face = "bottom"; Rect = [System.Drawing.Rectangle]::new($U + $Z + $X, $V, $X, $Z) }
    )
}

function Set-PixelSafe {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [int]$X,
        [int]$Y,
        [System.Drawing.Color]$Color
    )

    if ($X -lt 0 -or $Y -lt 0 -or $X -ge $Bitmap.Width -or $Y -ge $Bitmap.Height) {
        return
    }

    $Bitmap.SetPixel($X, $Y, $Color)
}

function Fill-GradientFace {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [System.Drawing.Rectangle]$Rect,
        [System.Drawing.Color]$BaseColor,
        [string]$Face
    )

    if ($Rect.Width -le 0 -or $Rect.Height -le 0) { return }

    switch ($Face) {
        "top" { $faceBias = 14 }
        "left" { $faceBias = 8 }
        "front" { $faceBias = 4 }
        "right" { $faceBias = -8 }
        "back" { $faceBias = -12 }
        "bottom" { $faceBias = -18 }
        default { $faceBias = 0 }
    }

    for ($x = 0; $x -lt $Rect.Width; $x++) {
        for ($y = 0; $y -lt $Rect.Height; $y++) {
            $light = [int](6 - ($x * 8 / [Math]::Max(1, $Rect.Width - 1)) - ($y * 10 / [Math]::Max(1, $Rect.Height - 1)))
            $color = Adjust-Color -Color $BaseColor -R ($faceBias + $light) -G ($faceBias + $light) -B ($faceBias + $light)
            Set-PixelSafe -Bitmap $Bitmap -X ($Rect.X + $x) -Y ($Rect.Y + $y) -Color $color
        }
    }

    $border = Adjust-Color -Color $BaseColor -R -18 -G -18 -B -18
    for ($x = $Rect.X; $x -lt ($Rect.X + $Rect.Width); $x++) {
        Set-PixelSafe -Bitmap $Bitmap -X $x -Y $Rect.Y -Color (Adjust-Color -Color $BaseColor -R 10 -G 10 -B 10)
        Set-PixelSafe -Bitmap $Bitmap -X $x -Y ($Rect.Y + $Rect.Height - 1) -Color $border
    }
    for ($y = $Rect.Y; $y -lt ($Rect.Y + $Rect.Height); $y++) {
        Set-PixelSafe -Bitmap $Bitmap -X $Rect.X -Y $y -Color (Adjust-Color -Color $BaseColor -R 8 -G 8 -B 8)
        Set-PixelSafe -Bitmap $Bitmap -X ($Rect.X + $Rect.Width - 1) -Y $y -Color $border
    }
}

function Draw-FoldBand {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [System.Drawing.Rectangle]$Rect,
        [System.Drawing.Color]$LightColor,
        [System.Drawing.Color]$DarkColor,
        [int[]]$Offsets
    )

    if ($Rect.Width -lt 3 -or $Rect.Height -lt 3) { return }

    foreach ($offset in $Offsets) {
        for ($x = 1; $x -lt ($Rect.Width - 1); $x++) {
            $y = [int]([Math]::Round(($x * 0.45) + $offset))
            if ($y -ge 1 -and $y -lt ($Rect.Height - 1)) {
                Set-PixelSafe -Bitmap $Bitmap -X ($Rect.X + $x) -Y ($Rect.Y + $y) -Color $DarkColor
                if ($y -gt 1) {
                    Set-PixelSafe -Bitmap $Bitmap -X ($Rect.X + $x) -Y ($Rect.Y + $y - 1) -Color $LightColor
                }
            }
        }
    }
}

function Draw-VerticalCreases {
    param(
        [System.Drawing.Bitmap]$Bitmap,
        [System.Drawing.Rectangle]$Rect,
        [System.Drawing.Color]$LightColor,
        [System.Drawing.Color]$DarkColor
    )

    if ($Rect.Width -lt 4 -or $Rect.Height -lt 4) { return }

    $cols = @([Math]::Max(1, [int]($Rect.Width / 3)), [Math]::Max(2, [int]($Rect.Width * 2 / 3)))
    foreach ($col in $cols) {
        for ($y = 1; $y -lt ($Rect.Height - 1); $y++) {
            Set-PixelSafe -Bitmap $Bitmap -X ($Rect.X + $col) -Y ($Rect.Y + $y) -Color $DarkColor
            Set-PixelSafe -Bitmap $Bitmap -X ($Rect.X + $col - 1) -Y ($Rect.Y + $y) -Color $LightColor
        }
    }
}

$bitmap = [System.Drawing.Bitmap]::FromFile($basePath)

foreach ($bone in $geometry.bones) {
    if (-not $bone.cubes) { continue }
    $baseColor = Get-BoneBaseColor -BoneName $bone.name

    foreach ($cube in $bone.cubes) {
        $u = [int]$cube.uv[0]
        $v = [int]$cube.uv[1]
        $x = [int]$cube.size[0]
        $y = [int]$cube.size[1]
        $z = [int]$cube.size[2]

        foreach ($face in (Get-FaceRects -U $u -V $v -X $x -Y $y -Z $z)) {
            Fill-GradientFace -Bitmap $bitmap -Rect $face.Rect -BaseColor $baseColor -Face $face.Face
        }
    }
}

$visorBright = [System.Drawing.Color]::FromArgb(255, 104, 218, 226)
$visorMid = [System.Drawing.Color]::FromArgb(255, 78, 186, 200)
$visorDark = [System.Drawing.Color]::FromArgb(255, 40, 116, 130)
$redGlow = [System.Drawing.Color]::FromArgb(255, 255, 96, 88)
$red = [System.Drawing.Color]::FromArgb(255, 228, 38, 44)
$maskDark = [System.Drawing.Color]::FromArgb(255, 10, 12, 16)
$hoodEdge = [System.Drawing.Color]::FromArgb(255, 56, 58, 66)

for ($x = 43; $x -le 47; $x++) {
    Set-PixelSafe -Bitmap $bitmap -X $x -Y 25 -Color $visorBright
    Set-PixelSafe -Bitmap $bitmap -X $x -Y 26 -Color $visorMid
}
Set-PixelSafe -Bitmap $bitmap -X 53 -Y 25 -Color $visorMid
Set-PixelSafe -Bitmap $bitmap -X 53 -Y 26 -Color $visorDark

Set-PixelSafe -Bitmap $bitmap -X 61 -Y 25 -Color $redGlow
Set-PixelSafe -Bitmap $bitmap -X 62 -Y 25 -Color $redGlow
Set-PixelSafe -Bitmap $bitmap -X 61 -Y 26 -Color $red
Set-PixelSafe -Bitmap $bitmap -X 62 -Y 26 -Color $red
Set-PixelSafe -Bitmap $bitmap -X 67 -Y 25 -Color $redGlow
Set-PixelSafe -Bitmap $bitmap -X 68 -Y 25 -Color $redGlow
Set-PixelSafe -Bitmap $bitmap -X 67 -Y 26 -Color $red
Set-PixelSafe -Bitmap $bitmap -X 68 -Y 26 -Color $red

for ($y = 25; $y -le 28; $y++) {
    Set-PixelSafe -Bitmap $bitmap -X 37 -Y $y -Color $maskDark
}
for ($x = 23; $x -le 28; $x++) {
    Set-PixelSafe -Bitmap $bitmap -X $x -Y 25 -Color $hoodEdge
}

$metalLight = [System.Drawing.Color]::FromArgb(255, 86, 88, 98)
$metalDark = [System.Drawing.Color]::FromArgb(255, 25, 25, 30)
for ($x = 67; $x -le 71; $x++) { Set-PixelSafe -Bitmap $bitmap -X $x -Y 1 -Color $metalLight }
for ($x = 67; $x -le 71; $x++) { Set-PixelSafe -Bitmap $bitmap -X $x -Y 9 -Color $metalLight }
for ($x = 88; $x -le 91; $x++) { Set-PixelSafe -Bitmap $bitmap -X $x -Y 2 -Color $metalDark }
for ($x = 88; $x -le 91; $x++) { Set-PixelSafe -Bitmap $bitmap -X $x -Y 10 -Color $metalDark }

$crimson = [System.Drawing.Color]::FromArgb(255, 128, 46, 52)
$crimsonLight = [System.Drawing.Color]::FromArgb(255, 157, 72, 79)
$crimsonDark = [System.Drawing.Color]::FromArgb(255, 86, 25, 31)
for ($x = 72; $x -le 76; $x++) {
    Set-PixelSafe -Bitmap $bitmap -X $x -Y 58 -Color $crimsonLight
    Set-PixelSafe -Bitmap $bitmap -X $x -Y 61 -Color $crimsonDark
}
for ($x = 53; $x -le 60; $x++) {
    Set-PixelSafe -Bitmap $bitmap -X $x -Y 40 -Color $crimsonLight
    Set-PixelSafe -Bitmap $bitmap -X $x -Y 41 -Color $crimson
}

$clothLight = [System.Drawing.Color]::FromArgb(255, 44, 45, 54)
$clothDark = [System.Drawing.Color]::FromArgb(255, 8, 8, 12)
$overlayLight = [System.Drawing.Color]::FromArgb(255, 72, 72, 82)
$overlayDark = [System.Drawing.Color]::FromArgb(255, 20, 20, 26)
$skinLight = [System.Drawing.Color]::FromArgb(255, 114, 165, 171)
$skinDark = [System.Drawing.Color]::FromArgb(255, 60, 96, 102)

Draw-FoldBand -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(4, 44, 8, 12)) -LightColor $clothLight -DarkColor $clothDark -Offsets @(2,6)
Draw-FoldBand -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(40, 59, 6, 10)) -LightColor $overlayLight -DarkColor $overlayDark -Offsets @(1,4)
Draw-FoldBand -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(57, 62, 5, 9)) -LightColor $overlayLight -DarkColor $overlayDark -Offsets @(0,3)
Draw-FoldBand -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(18, 79, 5, 7)) -LightColor $clothLight -DarkColor $clothDark -Offsets @(1,3)
Draw-FoldBand -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(18, 91, 5, 7)) -LightColor $clothLight -DarkColor $clothDark -Offsets @(1,3)
Draw-FoldBand -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(91, 79, 5, 6)) -LightColor $clothLight -DarkColor $clothDark -Offsets @(1,3)
Draw-FoldBand -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(91, 91, 5, 6)) -LightColor $clothLight -DarkColor $clothDark -Offsets @(1,3)

Draw-VerticalCreases -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(72, 78, 4, 6)) -LightColor $clothLight -DarkColor $clothDark
Draw-VerticalCreases -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(72, 90, 4, 6)) -LightColor $clothLight -DarkColor $clothDark

Draw-VerticalCreases -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(54, 76, 3, 3)) -LightColor $skinLight -DarkColor $skinDark
Draw-VerticalCreases -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(54, 88, 3, 3)) -LightColor $skinLight -DarkColor $skinDark
Draw-VerticalCreases -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(108, 77, 3, 8)) -LightColor $skinLight -DarkColor $skinDark
Draw-VerticalCreases -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(108, 89, 3, 8)) -LightColor $skinLight -DarkColor $skinDark
Draw-FoldBand -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(6, 103, 4, 3)) -LightColor $skinLight -DarkColor $skinDark -Offsets @(0)
Draw-FoldBand -Bitmap $bitmap -Rect ([System.Drawing.Rectangle]::new(26, 103, 4, 3)) -LightColor $skinLight -DarkColor $skinDark -Offsets @(0)

for ($x = 56; $x -le 58; $x++) {
    Set-PixelSafe -Bitmap $bitmap -X $x -Y 42 -Color $crimsonDark
    Set-PixelSafe -Bitmap $bitmap -X $x -Y 43 -Color $crimsonDark
}
for ($y = 63; $y -le 66; $y++) {
    Set-PixelSafe -Bitmap $bitmap -X 28 -Y $y -Color $crimson
    Set-PixelSafe -Bitmap $bitmap -X 29 -Y $y -Color $crimsonDark
}

$used = New-Object 'bool[,]' $bitmap.Width, $bitmap.Height
foreach ($bone in $geometry.bones) {
    if (-not $bone.cubes) { continue }
    foreach ($cube in $bone.cubes) {
        foreach ($face in (Get-FaceRects -U ([int]$cube.uv[0]) -V ([int]$cube.uv[1]) -X ([int]$cube.size[0]) -Y ([int]$cube.size[1]) -Z ([int]$cube.size[2]))) {
            for ($x = $face.Rect.X; $x -lt ($face.Rect.X + $face.Rect.Width); $x++) {
                for ($y = $face.Rect.Y; $y -lt ($face.Rect.Y + $face.Rect.Height); $y++) {
                    if ($x -ge 0 -and $y -ge 0 -and $x -lt $bitmap.Width -and $y -lt $bitmap.Height) {
                        $used[$x,$y] = $true
                    }
                }
            }
        }
    }
}
for ($x = 0; $x -lt $bitmap.Width; $x++) {
    for ($y = 0; $y -lt $bitmap.Height; $y++) {
        if (-not $used[$x,$y]) {
            $bitmap.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
        }
    }
}

$bitmap.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bitmap.Dispose()
Write-Output $outPath
