Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"

$workspace = "E:\desk\1.21.1NeoForge"
$geoPath = Join-Path $workspace "src\main\resources\assets\chen_mod\geo\dragon_brutel.geo.json"
$outputPath = Join-Path $workspace "src\main\resources\assets\chen_mod\textures\entity\dragon_brutel.png"

function New-Color([int]$r, [int]$g, [int]$b, [int]$a = 255) {
    return [System.Drawing.Color]::FromArgb($a, $r, $g, $b)
}

function Fill-Rect($graphics, $color, [int]$x, [int]$y, [int]$w, [int]$h) {
    if ($w -le 0 -or $h -le 0) {
        return
    }
    $brush = New-Object System.Drawing.SolidBrush($color)
    try {
        $graphics.FillRectangle($brush, $x, $y, $w, $h)
    } finally {
        $brush.Dispose()
    }
}

function Stroke-Rect($graphics, $color, [int]$x, [int]$y, [int]$w, [int]$h) {
    if ($w -le 0 -or $h -le 0) {
        return
    }
    $pen = New-Object System.Drawing.Pen($color)
    try {
        $graphics.DrawRectangle($pen, $x, $y, [Math]::Max(1, $w - 1), [Math]::Max(1, $h - 1))
    } finally {
        $pen.Dispose()
    }
}

function Get-CubeFaces([int]$u, [int]$v, [int]$sx, [int]$sy, [int]$sz) {
    return @{
        left   = @{ x = $u; y = $v + $sz; w = $sz; h = $sy }
        front  = @{ x = $u + $sz; y = $v + $sz; w = $sx; h = $sy }
        right  = @{ x = $u + $sz + $sx; y = $v + $sz; w = $sz; h = $sy }
        back   = @{ x = $u + $sz + $sx + $sz; y = $v + $sz; w = $sx; h = $sy }
        top    = @{ x = $u + $sz; y = $v; w = $sx; h = $sz }
        bottom = @{ x = $u + $sz + $sx; y = $v; w = $sx; h = $sz }
    }
}

function Paint-FaceBase($graphics, $face, $base, $shadow, $highlight, $outline) {
    Fill-Rect $graphics $base $face.x $face.y $face.w $face.h
    if ($face.h -ge 4) {
        Fill-Rect $graphics $highlight $face.x $face.y $face.w 1
        Fill-Rect $graphics $shadow $face.x ($face.y + $face.h - 1) $face.w 1
    }
    if ($face.w -ge 4) {
        Fill-Rect $graphics $shadow $face.x $face.y 1 $face.h
        Fill-Rect $graphics $outline ($face.x + $face.w - 1) $face.y 1 $face.h
    }
}

function Paint-Cube($graphics, $faces, $base, $shadow, $highlight, $outline) {
    foreach ($name in @("left", "front", "right", "back", "top", "bottom")) {
        Paint-FaceBase $graphics $faces[$name] $base $shadow $highlight $outline
    }
}

function Add-HorizontalPlates($graphics, $face, $color, $shadow) {
    if ($face.w -lt 4 -or $face.h -lt 4) {
        return
    }
    $rows = [Math]::Min(5, [Math]::Floor(($face.h - 1) / 2))
    for ($i = 0; $i -lt $rows; $i++) {
        $y = $face.y + 1 + ($i * 2)
        $inset = [Math]::Min([Math]::Floor($i / 2), [Math]::Floor(($face.w - 2) / 4))
        $width = [Math]::Max(2, $face.w - ($inset * 2))
        Fill-Rect $graphics $color ($face.x + $inset) $y $width 1
        if ($y + 1 -lt ($face.y + $face.h)) {
            Fill-Rect $graphics $shadow ($face.x + $inset + 1) ($y + 1) ([Math]::Max(1, $width - 2)) 1
        }
    }
}

function Add-BackSpineLine($graphics, $face, $color) {
    if ($face.w -lt 3 -or $face.h -lt 4) {
        return
    }
    $x = $face.x + [Math]::Floor($face.w / 2)
    Fill-Rect $graphics $color $x ($face.y + 1) 1 ([Math]::Max(1, $face.h - 2))
}

function Paint-EyeCube($graphics, $faces, $eye, $glow, $shadow, $outline) {
    foreach ($name in @("left", "right", "top", "bottom")) {
        Paint-FaceBase $graphics $faces[$name] $shadow $shadow $glow $outline
    }
    foreach ($name in @("front", "back")) {
        $face = $faces[$name]
        Fill-Rect $graphics $shadow $face.x $face.y $face.w $face.h
        if ($face.w -ge 3 -and $face.h -ge 2) {
            Fill-Rect $graphics $eye ($face.x + 1) $face.y ([Math]::Max(1, $face.w - 1)) $face.h
            Fill-Rect $graphics $glow ($face.x + 1) $face.y 1 1
            if ($face.w -ge 4) {
                Fill-Rect $graphics $glow ($face.x + 2) $face.y 1 1
            }
        } else {
            Fill-Rect $graphics $eye $face.x $face.y $face.w $face.h
        }
        if ($face.w -ge 3) {
            Fill-Rect $graphics $outline $face.x $face.y 1 $face.h
        }
        if ($face.h -ge 2) {
            Fill-Rect $graphics $outline $face.x ($face.y + $face.h - 1) $face.w 1
        }
    }
}

function Add-SnoutFront($graphics, $face, $nostril, $shadow) {
    if ($face.w -lt 4 -or $face.h -lt 3) {
        return
    }
    Fill-Rect $graphics $shadow ($face.x + 1) ($face.y + 1) ([Math]::Max(1, $face.w - 2)) 1
    Fill-Rect $graphics $nostril ($face.x + 1) ($face.y + $face.h - 1) 1 1
    Fill-Rect $graphics $nostril ($face.x + $face.w - 2) ($face.y + $face.h - 1) 1 1
}

function Add-Mouth($graphics, $face, $mouth, $teeth) {
    if ($face.w -lt 4 -or $face.h -lt 2) {
        return
    }
    Fill-Rect $graphics $mouth ($face.x + 1) $face.y ([Math]::Max(2, $face.w - 2)) $face.h
    Fill-Rect $graphics $teeth ($face.x + 1) $face.y 1 1
    Fill-Rect $graphics $teeth ($face.x + $face.w - 2) $face.y 1 1
}

function Add-ShortsHem($graphics, $face, $hem) {
    if ($face.h -lt 2) {
        return
    }
    Fill-Rect $graphics $hem $face.x ($face.y + $face.h - 2) $face.w 2
}

function Add-LoinSeam($graphics, $face, $seam) {
    $x = $face.x + [Math]::Floor($face.w / 2)
    Fill-Rect $graphics $seam $x $face.y 1 $face.h
}

function Add-FootClaws($graphics, $face, $claw, $shadow) {
    if ($face.w -lt 5 -or $face.h -lt 2) {
        return
    }
    $toeWidth = [Math]::Max(1, [Math]::Floor($face.w / 4))
    $positions = @(
        ($face.x + 1),
        ($face.x + [Math]::Floor($face.w / 2) - 1),
        ($face.x + $face.w - $toeWidth - 1)
    )
    foreach ($toeX in $positions) {
        Fill-Rect $graphics $shadow $toeX ($face.y + $face.h - 2) $toeWidth 1
        Fill-Rect $graphics $claw $toeX ($face.y + $face.h - 1) $toeWidth 1
    }
}

function Add-TailStripe($graphics, $face, $stripe) {
    if ($face.w -lt 2 -or $face.h -lt 4) {
        return
    }
    $centerX = $face.x + [Math]::Floor($face.w / 2)
    Fill-Rect $graphics $stripe $centerX $face.y 1 $face.h
}

$outline = New-Color 74 94 40
$deepShadow = New-Color 58 74 31
$oliveDark = New-Color 88 108 47
$olive = New-Color 108 130 56
$oliveLight = New-Color 136 160 80
$plate = New-Color 146 170 86
$plateShadow = New-Color 110 131 61
$horn = New-Color 224 231 191
$hornShadow = New-Color 148 153 113
$eyeShadow = New-Color 96 14 18
$eye = New-Color 228 24 38
$eyeGlow = New-Color 255 82 82
$mouth = New-Color 90 16 18
$mouthDark = New-Color 39 8 12
$shorts = New-Color 94 63 116
$shortsLight = New-Color 118 84 141
$shortsShadow = New-Color 60 37 79
$loin = New-Color 132 68 72
$loinShadow = New-Color 83 37 41
$belt = New-Color 42 33 44
$claw = New-Color 230 232 195
$clawShade = New-Color 165 168 133

$model = Get-Content -Raw $geoPath | ConvertFrom-Json
$textureWidth = [int]$model.'minecraft:geometry'[0].description.texture_width
$textureHeight = [int]$model.'minecraft:geometry'[0].description.texture_height

$bitmap = New-Object System.Drawing.Bitmap($textureWidth, $textureHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
try {
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None

        foreach ($bone in $model.'minecraft:geometry'[0].bones) {
            if (-not $bone.cubes) {
                continue
            }

            for ($cubeIndex = 0; $cubeIndex -lt $bone.cubes.Count; $cubeIndex++) {
                $cube = $bone.cubes[$cubeIndex]
                $u = [int]$cube.uv[0]
                $v = [int]$cube.uv[1]
                $sx = [int][Math]::Round([double]$cube.size[0], 0, [MidpointRounding]::AwayFromZero)
                $sy = [int][Math]::Round([double]$cube.size[1], 0, [MidpointRounding]::AwayFromZero)
                $sz = [int][Math]::Round([double]$cube.size[2], 0, [MidpointRounding]::AwayFromZero)
                if ($sx -le 0 -or $sy -le 0 -or $sz -le 0) {
                    continue
                }

                $faces = Get-CubeFaces $u $v $sx $sy $sz

                switch ($bone.name) {
                    "pelvis" {
                        Paint-Cube $graphics $faces $olive $oliveDark $oliveLight $outline
                        foreach ($side in @("left", "front", "right", "back")) {
                            $face = $faces[$side]
                            $overlayY = $face.y + [Math]::Floor($face.h / 2)
                            Fill-Rect $graphics $shorts $face.x $overlayY $face.w ($face.y + $face.h - $overlayY)
                            Add-ShortsHem $graphics $face $loinShadow
                        }
                    }
                    "belt" {
                        Paint-Cube $graphics $faces $belt $outline $shortsShadow $outline
                    }
                    "loin_front" {
                        Paint-Cube $graphics $faces $loin $loinShadow $shortsLight $outline
                        Add-LoinSeam $graphics $faces.front $belt
                    }
                    "loin_back" {
                        Paint-Cube $graphics $faces $loin $loinShadow $shortsLight $outline
                        Add-LoinSeam $graphics $faces.back $belt
                    }
                    "torso" {
                        if ($cubeIndex -lt 2) {
                            Paint-Cube $graphics $faces $olive $oliveDark $oliveLight $outline
                            Add-HorizontalPlates $graphics $faces.front $plate $plateShadow
                            Add-HorizontalPlates $graphics $faces.top $plate $plateShadow
                            Add-BackSpineLine $graphics $faces.back $deepShadow
                        } else {
                            Paint-Cube $graphics $faces $oliveDark $deepShadow $olive $outline
                            Add-BackSpineLine $graphics $faces.back $plate
                        }
                    }
                    "chest_plates" {
                        Paint-Cube $graphics $faces $plate $plateShadow $oliveLight $outline
                    }
                    "back_spines" {
                        Paint-Cube $graphics $faces $oliveDark $deepShadow $olive $outline
                        Fill-Rect $graphics $horn ($faces.front.x + [Math]::Floor($faces.front.w / 2)) $faces.front.y 1 ([Math]::Max(1, $faces.front.h - 1))
                    }
                    "neck" {
                        if ($cubeIndex -eq 0) {
                            Paint-Cube $graphics $faces $olive $oliveDark $oliveLight $outline
                            Add-HorizontalPlates $graphics $faces.front $plate $plateShadow
                        } elseif ($cubeIndex -eq 1) {
                            Paint-Cube $graphics $faces $plate $plateShadow $oliveLight $outline
                        } else {
                            Paint-Cube $graphics $faces $oliveDark $deepShadow $olive $outline
                        }
                    }
                    "head" {
                        if ($cubeIndex -eq 0) {
                            Paint-Cube $graphics $faces $olive $oliveDark $oliveLight $outline
                            Add-BackSpineLine $graphics $faces.top $deepShadow
                        } elseif ($cubeIndex -eq 1) {
                            Paint-Cube $graphics $faces $oliveDark $deepShadow $olive $outline
                            Add-SnoutFront $graphics $faces.front $mouthDark $deepShadow
                            Add-SnoutFront $graphics $faces.left $mouthDark $deepShadow
                            Add-SnoutFront $graphics $faces.right $mouthDark $deepShadow
                        } elseif ($cubeIndex -in @(2, 3)) {
                            Paint-EyeCube $graphics $faces $eye $eyeGlow $eyeShadow $outline
                        } else {
                            Paint-Cube $graphics $faces $oliveDark $deepShadow $olive $outline
                            Add-SnoutFront $graphics $faces.front $mouthDark $deepShadow
                        }
                    }
                    "jaw" {
                        if ($cubeIndex -eq 0) {
                            Paint-Cube $graphics $faces $oliveDark $deepShadow $olive $outline
                            Add-Mouth $graphics $faces.front $mouth $claw
                        } else {
                            Paint-Cube $graphics $faces $horn $hornShadow $claw $outline
                        }
                    }
                    { $_ -in @("left_horn", "right_horn") } {
                        Paint-Cube $graphics $faces $horn $hornShadow $claw $outline
                    }
                    { $_ -in @("left_head_spikes", "right_head_spikes") } {
                        Paint-Cube $graphics $faces $oliveDark $deepShadow $olive $outline
                        Fill-Rect $graphics $horn ($faces.front.x + [Math]::Floor($faces.front.w / 2)) $faces.front.y 1 ([Math]::Max(1, $faces.front.h - 1))
                    }
                    { $_ -in @("left_arm", "left_forearm", "right_arm", "right_forearm", "left_thigh", "right_thigh", "left_shin", "right_shin") } {
                        Paint-Cube $graphics $faces $olive $oliveDark $oliveLight $outline
                    }
                    { $_ -in @("left_hand", "right_hand") } {
                        if ($cubeIndex -eq 0) {
                            Paint-Cube $graphics $faces $olive $oliveDark $oliveLight $outline
                        } else {
                            Paint-Cube $graphics $faces $horn $hornShadow $claw $outline
                        }
                    }
                    { $_ -in @("left_foot", "right_foot") } {
                        if ($cubeIndex -lt 2) {
                            Paint-Cube $graphics $faces $olive $oliveDark $oliveLight $outline
                            if ($cubeIndex -eq 0) {
                                Add-FootClaws $graphics $faces.front $claw $clawShade
                            }
                        } else {
                            Paint-Cube $graphics $faces $horn $hornShadow $claw $outline
                        }
                    }
                    "tail" {
                        Paint-Cube $graphics $faces $olive $oliveDark $oliveLight $outline
                        Add-TailStripe $graphics $faces.top $deepShadow
                        Add-TailStripe $graphics $faces.back $deepShadow
                    }
                    default {
                        Paint-Cube $graphics $faces $olive $oliveDark $oliveLight $outline
                    }
                }
            }
        }

        $directory = Split-Path -Parent $outputPath
        if (-not (Test-Path $directory)) {
            New-Item -ItemType Directory -Path $directory | Out-Null
        }
        if (Test-Path $outputPath) {
            Remove-Item -LiteralPath $outputPath -Force
        }
        $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
    }
} finally {
    $bitmap.Dispose()
}

Write-Output $outputPath
