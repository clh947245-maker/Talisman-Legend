Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"

$workspace = "E:\desk\1.21.1NeoForge"
$sourceGeoPath = Join-Path $workspace "src\main\resources\assets\chen_mod\geo\dragon_brutel.geo.json"
$outputGeoPath = Join-Path $workspace "src\main\resources\assets\chen_mod\geo\sheng_zhu.geo.json"
$texturePath = Join-Path $workspace "src\main\resources\assets\chen_mod\textures\entity\sheng_zhu.png"
$glowmaskPath = Join-Path $workspace "src\main\resources\assets\chen_mod\textures\entity\sheng_zhu_glowmask.png"

function New-Cube {
    param(
        [double[]]$Origin,
        [double[]]$Size,
        [int[]]$Uv = @(0, 0),
        [double[]]$Pivot = $null,
        [double[]]$Rotation = $null,
        [switch]$Mirror
    )

    $cube = [ordered]@{
        origin = $Origin
        size   = $Size
        uv     = $Uv
    }

    if ($null -ne $Pivot) {
        $cube.pivot = $Pivot
    }

    if ($null -ne $Rotation) {
        $cube.rotation = $Rotation
    }

    if ($Mirror.IsPresent) {
        $cube.mirror = $true
    }

    return [pscustomobject]$cube
}

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

function Add-HorizontalPlates($graphics, $face, $plate, $plateShadow) {
    if ($face.w -lt 6 -or $face.h -lt 6) {
        return
    }

    $rows = [Math]::Min(6, [Math]::Floor(($face.h - 2) / 2))
    for ($i = 0; $i -lt $rows; $i++) {
        $y = $face.y + 1 + ($i * 2)
        $inset = [Math]::Min($i, [Math]::Floor(($face.w - 2) / 3))
        $width = [Math]::Max(2, $face.w - ($inset * 2))
        Fill-Rect $graphics $plate ($face.x + $inset) $y $width 1
        if ($y + 1 -lt ($face.y + $face.h)) {
            Fill-Rect $graphics $plateShadow ($face.x + $inset + 1) ($y + 1) ([Math]::Max(1, $width - 2)) 1
        }
    }
}

function Add-PlateCenterLine($graphics, $face, $lineColor) {
    if ($face.w -lt 3 -or $face.h -lt 4) {
        return
    }

    $x = $face.x + [Math]::Floor($face.w / 2)
    Fill-Rect $graphics $lineColor $x ($face.y + 1) 1 ([Math]::Max(1, $face.h - 2))
}

function Add-MuscleBands($graphics, $face, $highlight, $shadow) {
    if ($face.w -lt 5 -or $face.h -lt 6) {
        return
    }

    $centerX = $face.x + [Math]::Floor($face.w / 2)
    Fill-Rect $graphics $highlight $centerX ($face.y + 1) 1 ([Math]::Max(2, $face.h - 3))
    Fill-Rect $graphics $shadow $face.x ($face.y + [Math]::Floor($face.h / 3)) $face.w 1
    Fill-Rect $graphics $shadow $face.x ($face.y + [Math]::Floor(($face.h * 2) / 3)) $face.w 1
}

function Add-BackSpineLine($graphics, $face, $color) {
    if ($face.w -lt 3 -or $face.h -lt 4) {
        return
    }

    $x = $face.x + [Math]::Floor($face.w / 2)
    Fill-Rect $graphics $color $x ($face.y + 1) 1 ([Math]::Max(1, $face.h - 2))
}

function Add-SnoutFront($graphics, $face, $shadow, $nostril) {
    if ($face.w -lt 5 -or $face.h -lt 3) {
        return
    }

    Fill-Rect $graphics $shadow ($face.x + 1) ($face.y + 1) ([Math]::Max(1, $face.w - 2)) 1
    Fill-Rect $graphics $nostril ($face.x + 1) ($face.y + $face.h - 1) 1 1
    Fill-Rect $graphics $nostril ($face.x + $face.w - 2) ($face.y + $face.h - 1) 1 1
}

function Add-FaceStripe($graphics, $face, $shadow, $highlight) {
    if ($face.w -lt 4 -or $face.h -lt 5) {
        return
    }

    Fill-Rect $graphics $shadow ($face.x + 1) ($face.y + 1) 1 ([Math]::Max(2, $face.h - 2))
    Fill-Rect $graphics $highlight ($face.x + $face.w - 2) ($face.y + 1) 1 ([Math]::Max(2, $face.h - 2))
}

function Paint-EyeCube($graphics, $faces, $eye, $glow, $shadow, $outline) {
    foreach ($name in @("left", "right", "top", "bottom")) {
        Paint-FaceBase $graphics $faces[$name] $shadow $shadow $glow $outline
    }

    foreach ($name in @("front", "back")) {
        $face = $faces[$name]
        Fill-Rect $graphics $shadow $face.x $face.y $face.w $face.h
        Fill-Rect $graphics $eye ($face.x + 1) $face.y ([Math]::Max(1, $face.w - 1)) $face.h
        Fill-Rect $graphics $glow ($face.x + 1) $face.y 1 1
        if ($face.w -ge 4) {
            Fill-Rect $graphics $glow ($face.x + 2) $face.y 1 1
        }
    }
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

function Add-LoinSeam($graphics, $face, $color) {
    $x = $face.x + [Math]::Floor($face.w / 2)
    Fill-Rect $graphics $color $x $face.y 1 $face.h
}

function Add-FootClaws($graphics, $face, $claw, $shadow) {
    if ($face.w -lt 6 -or $face.h -lt 2) {
        return
    }

    $toeWidth = [Math]::Max(1, [Math]::Floor($face.w / 5))
    $positions = @(
        ($face.x + 1),
        ($face.x + [Math]::Floor($face.w / 2) - [Math]::Floor($toeWidth / 2)),
        ($face.x + $face.w - $toeWidth - 2)
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

function Paint-GlowCube($graphics, $faces, $glowColor) {
    foreach ($name in @("left", "front", "right", "back", "top", "bottom")) {
        $face = $faces[$name]
        Fill-Rect $graphics $glowColor $face.x $face.y $face.w $face.h
    }
}

$model = Get-Content -Raw $sourceGeoPath | ConvertFrom-Json
$geometry = $model.'minecraft:geometry'[0]
$geometry.description.identifier = "geometry.chen_mod.sheng_zhu"
$geometry.description.texture_width = 256
$geometry.description.texture_height = 256
$geometry.description.visible_bounds_width = 9.5
$geometry.description.visible_bounds_height = 7.2
$geometry.description.visible_bounds_offset = @(0, 3.1, 0)

$bones = $geometry.bones
$boneMap = @{}
foreach ($bone in $bones) {
    $boneMap[$bone.name] = $bone
}

$boneMap["pelvis"].cubes = @(
    (New-Cube @(-7, 32, -4) @(14, 9, 8)),
    (New-Cube @(-6, 39, -4.5) @(12, 7, 9))
)

$boneMap["belt"].pivot = @(0, 40, 0)
$boneMap["belt"].cubes = @(
    (New-Cube @(-7.5, 39, -4.7) @(15, 2, 9))
)

$boneMap["loin_front"].pivot = @(0, 39, -4.1)
$boneMap["loin_front"].cubes = @(
    (New-Cube @(-3.5, 24, -5) @(7, 14, 1) @(0, 39, -4.1) @(12, 0, 0))
)

$boneMap["loin_back"].pivot = @(0, 39, 4.1)
$boneMap["loin_back"].cubes = @(
    (New-Cube @(-3.5, 23, 4.1) @(7, 13, 1) @(0, 39, 4.1) @(-10, 0, 0))
)

$boneMap["torso"].pivot = @(0, 40, 0)
$boneMap["torso"].cubes = @(
    (New-Cube @(-8.5, 40, -4.5) @(17, 12, 9)),
    (New-Cube @(-10, 52, -6.5) @(20, 13, 13)),
    (New-Cube @(-8, 56, 5) @(16, 9, 3)),
    (New-Cube @(-7, 60, -2.5) @(14, 7, 5))
)

$boneMap["chest_plates"].pivot = @(0, 40, 0)
$boneMap["chest_plates"].cubes = @(
    (New-Cube @(-6, 42, -5.3) @(12, 2, 1)),
    (New-Cube @(-7, 46, -5.5) @(14, 2, 1)),
    (New-Cube @(-8, 50, -5.7) @(16, 2, 1)),
    (New-Cube @(-8, 54, -5.9) @(16, 2, 1)),
    (New-Cube @(-7, 58, -5.7) @(14, 2, 1)),
    (New-Cube @(-6, 62, -5.5) @(12, 2, 1)),
    (New-Cube @(-4, 66, -5.3) @(8, 2, 1)),
    (New-Cube @(-1, 44, -5.4) @(2, 18, 1))
)

$boneMap["back_spines"].pivot = @(0, 67, 6)
$boneMap["back_spines"].cubes = @(
    (New-Cube @(-1, 71, 5.2) @(2, 6, 2) @(0, 71, 6) @(28, 0, 0)),
    (New-Cube @(-1, 66, 5.2) @(2, 6, 2) @(0, 66, 6) @(24, 0, 0)),
    (New-Cube @(-1, 61, 5.2) @(2, 5, 2) @(0, 61, 6) @(20, 0, 0)),
    (New-Cube @(-1, 56, 5.2) @(2, 5, 2) @(0, 56, 6) @(16, 0, 0)),
    (New-Cube @(-1, 51, 5.2) @(2, 4, 2) @(0, 51, 6) @(12, 0, 0)),
    (New-Cube @(-1, 47, 5.2) @(2, 4, 2) @(0, 47, 6) @(10, 0, 0))
)

$boneMap["neck"].pivot = @(0, 65, 0)
$boneMap["neck"].cubes = @(
    (New-Cube @(-5.5, 61.5, -3.5) @(11, 9, 7)),
    (New-Cube @(-4.5, 62.5, -4.4) @(9, 8, 1)),
    (New-Cube @(-4, 68, -5.2) @(8, 5, 5)),
    (New-Cube @(-2, 70, -6.5) @(4, 2, 3))
)

$boneMap["head"].pivot = @(0, 71, -2)
$boneMap["head"].cubes = @(
    (New-Cube @(-4.5, 67, -6) @(9, 8, 10)),
    (New-Cube @(-3.5, 66, -12) @(7, 5, 7)),
    (New-Cube @(-3.70994, 72.14952, -7) @(3, 1.5, 1) @(-2.20994, 72.89952, -6.5) @(0, 0, -150)),
    (New-Cube @(0.29006, 72.14952, -7) @(3, 1.5, 1) @(1.79006, 72.89952, -6.5) @(0, 0, -30)),
    (New-Cube @(-2.5, 66, -13) @(5, 2, 3))
)

$boneMap["jaw"].pivot = @(0, 69, -6)
$boneMap["jaw"].cubes = @(
    (New-Cube @(-2.5, 65.4, -11.5) @(1, 1.3, 1)),
    (New-Cube @(-1.5, 65.4, -11.5) @(1, 1.3, 1)),
    (New-Cube @(-0.5, 65.4, -11.5) @(1, 1.3, 1)),
    (New-Cube @(0.5, 65.4, -11.5) @(1, 1.3, 1)),
    (New-Cube @(1.5, 65.4, -11.5) @(1, 1.3, 1))
)

$boneMap["left_horn"].pivot = @(3.6, 75, -1.5)
$boneMap["left_horn"].cubes = @(
    (New-Cube @(2.8, 74, -2.3) @(2, 4, 2) @(3.8, 74, -1.3) @(0, 0, -24)),
    (New-Cube @(3.2, 77, -1.8) @(1, 3, 1) @(3.7, 77, -1.3) @(0, 0, -38))
)

$boneMap["right_horn"].pivot = @(-3.6, 75, -1.5)
$boneMap["right_horn"].cubes = @(
    (New-Cube @(-4.8, 74, -2.3) @(2, 4, 2) @(-3.8, 74, -1.3) @(0, 0, 24)),
    (New-Cube @(-4.2, 77, -1.8) @(1, 3, 1) @(-3.7, 77, -1.3) @(0, 0, 38))
)

$boneMap["left_head_spikes"].pivot = @(4.5, 72, 0)
$boneMap["left_head_spikes"].cubes = @(
    (New-Cube @(4.2, 69, -5) @(1, 6, 2) @(4.5, 72, -4) @(0, 0, -110)),
    (New-Cube @(4.2, 67, -5) @(1, 6, 2) @(4.5, 70, -4) @(0, 0, -110)),
    (New-Cube @(4.2, 65, -5) @(1, 6, 2) @(4.5, 68, -4) @(0, 0, -110))
)

$boneMap["right_head_spikes"].pivot = @(-4.5, 72, 0)
$boneMap["right_head_spikes"].cubes = @(
    (New-Cube @(-5.2, 69, -5) @(1, 6, 2) @(-4.5, 72, -4) @(0, 0, 110) -Mirror),
    (New-Cube @(-5.2, 67, -5) @(1, 6, 2) @(-4.5, 70, -4) @(0, 0, 110) -Mirror),
    (New-Cube @(-5.2, 65, -5) @(1, 6, 2) @(-4.5, 68, -4) @(0, 0, 110) -Mirror)
)

$boneMap["mandible"].pivot = @(0, 64.62788, -5)
$boneMap["mandible"].cubes = @(
    (New-Cube @(-3, 63.62788, -12.04047) @(6, 2, 8) @(0, 64.62788, -8.04047) @(12, 0, 0))
)

$boneMap["left_arm"].pivot = @(9.5, 59, 0)
$boneMap["left_arm"].cubes = @(
    (New-Cube @(7, 54.5, -5.5) @(9, 8, 11)),
    (New-Cube @(8.5, 39, -3.5) @(7, 16, 7)),
    (New-Cube @(8, 41, -4) @(8, 9, 8))
)

$boneMap["left_forearm"].pivot = @(12, 39, 0)
$boneMap["left_forearm"].cubes = @(
    (New-Cube @(9, 24, -3) @(6, 15, 6)),
    (New-Cube @(8.5, 27, -3.5) @(7, 9, 7))
)

$boneMap["left_hand"].pivot = @(12, 24, 0)
$boneMap["left_hand"].cubes = @(
    (New-Cube @(8.5, 16, -3) @(6, 8, 6)),
    (New-Cube @(13.8, 12, -2.2) @(1, 5, 1) @(14.3, 15, -1.7) @(0, 0, -12)),
    (New-Cube @(14.1, 12, 0) @(1, 5, 1) @(14.6, 15, 0.5) @(0, 0, -5)),
    (New-Cube @(13.8, 12, 2.2) @(1, 5, 1) @(14.3, 15, 2.7) @(0, 0, 7))
)

$boneMap["right_arm"].pivot = @(-9.5, 59, 0)
$boneMap["right_arm"].cubes = @(
    (New-Cube @(-16, 54.5, -5.5) @(9, 8, 11)),
    (New-Cube @(-15.5, 39, -3.5) @(7, 16, 7)),
    (New-Cube @(-16, 41, -4) @(8, 9, 8))
)

$boneMap["right_forearm"].pivot = @(-12, 39, 0)
$boneMap["right_forearm"].cubes = @(
    (New-Cube @(-15, 24, -3) @(6, 15, 6)),
    (New-Cube @(-15.5, 27, -3.5) @(7, 9, 7))
)

$boneMap["right_hand"].pivot = @(-12, 24, 0)
$boneMap["right_hand"].cubes = @(
    (New-Cube @(-14.5, 16, -3) @(6, 8, 6)),
    (New-Cube @(-14.8, 12, -2.2) @(1, 5, 1) @(-14.3, 15, -1.7) @(0, 0, 12)),
    (New-Cube @(-15.1, 12, 0) @(1, 5, 1) @(-14.6, 15, 0.5) @(0, 0, 5)),
    (New-Cube @(-14.8, 12, 2.2) @(1, 5, 1) @(-14.3, 15, 2.7) @(0, 0, -7))
)

$boneMap["tail"].pivot = @(0, 34, 4.5)
$boneMap["tail"].cubes = @(
    (New-Cube @(-2.5, 27.5, 3.5) @(5, 11, 5) @(0, 32.5, 4.5) @(30, 0, 0)),
    (New-Cube @(-2, 18.5, 10) @(4, 13, 4) @(0, 24.5, 10.5) @(36, 10, 0)),
    (New-Cube @(1, 8.5, 18) @(3, 13, 3) @(2.5, 15.5, 18.5) @(42, 16, 0))
)

$boneMap["left_thigh"].pivot = @(4.5, 32, 0)
$boneMap["left_thigh"].cubes = @(
    (New-Cube @(1, 17, -3.5) @(7, 15, 7)),
    (New-Cube @(0.5, 19, -4) @(8, 9, 8))
)

$boneMap["left_shin"].pivot = @(4.5, 17, 1)
$boneMap["left_shin"].cubes = @(
    (New-Cube @(2, 3, -2) @(5, 15, 5))
)

$boneMap["left_foot"].pivot = @(4.5, 3, 1)
$boneMap["left_foot"].cubes = @(
    (New-Cube @(0.5, 0, -4) @(7, 4, 10)),
    (New-Cube @(1.5, -1, 5) @(5, 7, 3)),
    (New-Cube @(1.4, 0, -8.5) @(1, 2, 5)),
    (New-Cube @(3.6, 0, -8.5) @(1, 2, 5)),
    (New-Cube @(5.8, 0, -8.5) @(1, 2, 5))
)

$boneMap["right_thigh"].pivot = @(-4.5, 32, 0)
$boneMap["right_thigh"].cubes = @(
    (New-Cube @(-8, 17, -3.5) @(7, 15, 7)),
    (New-Cube @(-8.5, 19, -4) @(8, 9, 8))
)

$boneMap["right_shin"].pivot = @(-4.5, 17, 1)
$boneMap["right_shin"].cubes = @(
    (New-Cube @(-7, 3, -2) @(5, 15, 5))
)

$boneMap["right_foot"].pivot = @(-4.5, 3, 1)
$boneMap["right_foot"].cubes = @(
    (New-Cube @(-7.5, 0, -4) @(7, 4, 10)),
    (New-Cube @(-6.5, -1, 5) @(5, 7, 3)),
    (New-Cube @(-6.8, 0, -8.5) @(1, 2, 5)),
    (New-Cube @(-4.6, 0, -8.5) @(1, 2, 5)),
    (New-Cube @(-2.4, 0, -8.5) @(1, 2, 5))
)

$textureWidth = [int]$geometry.description.texture_width
$textureHeight = [int]$geometry.description.texture_height
$padding = 2
$cursorX = 0
$cursorY = 0
$rowHeight = 0

foreach ($bone in $bones) {
    if (-not $bone.cubes) {
        continue
    }

    foreach ($cube in $bone.cubes) {
        $sx = [int][Math]::Round([double]$cube.size[0], 0, [MidpointRounding]::AwayFromZero)
        $sy = [int][Math]::Round([double]$cube.size[1], 0, [MidpointRounding]::AwayFromZero)
        $sz = [int][Math]::Round([double]$cube.size[2], 0, [MidpointRounding]::AwayFromZero)

        $boxWidth = (2 * ($sx + $sz)) + $padding
        $boxHeight = ($sy + $sz) + $padding

        if (($cursorX + $boxWidth) -gt $textureWidth) {
            $cursorX = 0
            $cursorY += $rowHeight
            $rowHeight = 0
        }

        if (($cursorY + $boxHeight) -gt $textureHeight) {
            throw "Packed UVs exceed $textureWidth x $textureHeight texture size."
        }

        $cube.uv = @($cursorX, $cursorY)
        $cursorX += $boxWidth
        if ($boxHeight -gt $rowHeight) {
            $rowHeight = $boxHeight
        }
    }
}

$geoDirectory = Split-Path -Parent $outputGeoPath
if (-not (Test-Path $geoDirectory)) {
    New-Item -ItemType Directory -Path $geoDirectory | Out-Null
}

$textureDirectory = Split-Path -Parent $texturePath
if (-not (Test-Path $textureDirectory)) {
    New-Item -ItemType Directory -Path $textureDirectory | Out-Null
}

$json = $model | ConvertTo-Json -Depth 100
[System.IO.File]::WriteAllText($outputGeoPath, $json, [System.Text.Encoding]::UTF8)

$outline = New-Color 49 64 22
$deepShadow = New-Color 28 38 12
$scaleDark = New-Color 63 84 28
$scaleBase = New-Color 89 117 41
$scaleLight = New-Color 128 160 67
$plate = New-Color 145 171 78
$plateShadow = New-Color 103 122 48
$plateHighlight = New-Color 185 205 104
$horn = New-Color 214 221 182
$hornShadow = New-Color 137 144 110
$eyeShadow = New-Color 82 10 12
$eye = New-Color 225 26 36
$eyeGlow = New-Color 255 92 86
$mouth = New-Color 102 18 20
$mouthDark = New-Color 46 10 12
$shorts = New-Color 72 42 84
$shortsLight = New-Color 100 62 113
$shortsShadow = New-Color 44 24 51
$loin = New-Color 118 52 56
$loinShadow = New-Color 74 28 31
$beltColor = New-Color 33 28 35
$claw = New-Color 229 231 198
$clawShade = New-Color 170 173 135

$bitmap = New-Object System.Drawing.Bitmap($textureWidth, $textureHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$glowmask = New-Object System.Drawing.Bitmap($textureWidth, $textureHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

try {
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $glowGraphics = [System.Drawing.Graphics]::FromImage($glowmask)

    try {
        foreach ($g in @($graphics, $glowGraphics)) {
            $g.Clear([System.Drawing.Color]::Transparent)
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        }

        foreach ($bone in $bones) {
            if (-not $bone.cubes) {
                continue
            }

            for ($cubeIndex = 0; $cubeIndex -lt $bone.cubes.Count; $cubeIndex++) {
                $cube = $bone.cubes[$cubeIndex]
                $sx = [int][Math]::Round([double]$cube.size[0], 0, [MidpointRounding]::AwayFromZero)
                $sy = [int][Math]::Round([double]$cube.size[1], 0, [MidpointRounding]::AwayFromZero)
                $sz = [int][Math]::Round([double]$cube.size[2], 0, [MidpointRounding]::AwayFromZero)
                $u = [int]$cube.uv[0]
                $v = [int]$cube.uv[1]
                $faces = Get-CubeFaces $u $v $sx $sy $sz

                switch ($bone.name) {
                    "pelvis" {
                        Paint-Cube $graphics $faces $scaleBase $scaleDark $scaleLight $outline
                        foreach ($side in @("left", "front", "right", "back")) {
                            $face = $faces[$side]
                            $overlayY = $face.y + [Math]::Floor($face.h / 2)
                            Fill-Rect $graphics $shorts $face.x $overlayY $face.w ($face.y + $face.h - $overlayY)
                            Add-ShortsHem $graphics $face $loinShadow
                        }
                    }
                    "belt" {
                        Paint-Cube $graphics $faces $beltColor $outline $shortsShadow $outline
                    }
                    "loin_front" {
                        Paint-Cube $graphics $faces $loin $loinShadow $shortsLight $outline
                        Add-LoinSeam $graphics $faces.front $beltColor
                    }
                    "loin_back" {
                        Paint-Cube $graphics $faces $loin $loinShadow $shortsLight $outline
                        Add-LoinSeam $graphics $faces.back $beltColor
                    }
                    "torso" {
                        if ($cubeIndex -lt 2) {
                            Paint-Cube $graphics $faces $scaleBase $scaleDark $scaleLight $outline
                            Add-HorizontalPlates $graphics $faces.front $plate $plateShadow
                            Add-MuscleBands $graphics $faces.left $scaleLight $deepShadow
                            Add-MuscleBands $graphics $faces.right $scaleLight $deepShadow
                            Add-BackSpineLine $graphics $faces.back $deepShadow
                        } elseif ($cubeIndex -eq 2) {
                            Paint-Cube $graphics $faces $scaleDark $deepShadow $scaleBase $outline
                            Add-BackSpineLine $graphics $faces.back $plate
                        } else {
                            Paint-Cube $graphics $faces $scaleDark $deepShadow $scaleLight $outline
                            Add-MuscleBands $graphics $faces.front $scaleLight $deepShadow
                        }
                    }
                    "chest_plates" {
                        Paint-Cube $graphics $faces $plate $plateShadow $plateHighlight $outline
                        Add-PlateCenterLine $graphics $faces.front $plateHighlight
                    }
                    "back_spines" {
                        Paint-Cube $graphics $faces $scaleDark $deepShadow $scaleBase $outline
                        Fill-Rect $graphics $horn ($faces.front.x + [Math]::Floor($faces.front.w / 2)) $faces.front.y 1 ([Math]::Max(1, $faces.front.h - 1))
                    }
                    "neck" {
                        if ($cubeIndex -eq 0) {
                            Paint-Cube $graphics $faces $scaleBase $scaleDark $scaleLight $outline
                            Add-HorizontalPlates $graphics $faces.front $plate $plateShadow
                            Add-MuscleBands $graphics $faces.left $scaleLight $deepShadow
                            Add-MuscleBands $graphics $faces.right $scaleLight $deepShadow
                        } elseif ($cubeIndex -eq 1) {
                            Paint-Cube $graphics $faces $plate $plateShadow $plateHighlight $outline
                        } else {
                            Paint-Cube $graphics $faces $scaleDark $deepShadow $scaleBase $outline
                            Add-BackSpineLine $graphics $faces.top $plate
                        }
                    }
                    "head" {
                        if ($cubeIndex -eq 0) {
                            Paint-Cube $graphics $faces $scaleBase $scaleDark $scaleLight $outline
                            Add-BackSpineLine $graphics $faces.top $deepShadow
                            Add-FaceStripe $graphics $faces.left $deepShadow $scaleLight
                            Add-FaceStripe $graphics $faces.right $deepShadow $scaleLight
                        } elseif ($cubeIndex -eq 1) {
                            Paint-Cube $graphics $faces $scaleDark $deepShadow $scaleBase $outline
                            Add-SnoutFront $graphics $faces.front $deepShadow $mouthDark
                            Add-SnoutFront $graphics $faces.left $deepShadow $mouthDark
                            Add-SnoutFront $graphics $faces.right $deepShadow $mouthDark
                        } elseif ($cubeIndex -in @(2, 3)) {
                            Paint-EyeCube $graphics $faces $eye $eyeGlow $eyeShadow $outline
                            Paint-GlowCube $glowGraphics $faces (New-Color 255 58 42 255)
                        } else {
                            Paint-Cube $graphics $faces $scaleDark $deepShadow $scaleBase $outline
                        }
                    }
                    "jaw" {
                        Paint-Cube $graphics $faces $horn $hornShadow $claw $outline
                    }
                    "mandible" {
                        Paint-Cube $graphics $faces $scaleDark $deepShadow $scaleBase $outline
                        Add-Mouth $graphics $faces.front $mouth $claw
                    }
                    { $_ -in @("left_horn", "right_horn") } {
                        Paint-Cube $graphics $faces $horn $hornShadow $claw $outline
                    }
                    { $_ -in @("left_head_spikes", "right_head_spikes") } {
                        Paint-Cube $graphics $faces $scaleDark $deepShadow $scaleBase $outline
                        Fill-Rect $graphics $horn ($faces.front.x + [Math]::Floor($faces.front.w / 2)) $faces.front.y 1 ([Math]::Max(1, $faces.front.h - 1))
                    }
                    { $_ -in @("left_arm", "left_forearm", "right_arm", "right_forearm", "left_thigh", "right_thigh", "left_shin", "right_shin") } {
                        Paint-Cube $graphics $faces $scaleBase $scaleDark $scaleLight $outline
                        Add-MuscleBands $graphics $faces.front $scaleLight $deepShadow
                    }
                    { $_ -in @("left_hand", "right_hand") } {
                        if ($cubeIndex -eq 0) {
                            Paint-Cube $graphics $faces $scaleBase $scaleDark $scaleLight $outline
                            Add-MuscleBands $graphics $faces.front $scaleLight $deepShadow
                        } else {
                            Paint-Cube $graphics $faces $horn $hornShadow $claw $outline
                        }
                    }
                    { $_ -in @("left_foot", "right_foot") } {
                        if ($cubeIndex -lt 2) {
                            Paint-Cube $graphics $faces $scaleBase $scaleDark $scaleLight $outline
                            if ($cubeIndex -eq 0) {
                                Add-FootClaws $graphics $faces.front $claw $clawShade
                            }
                        } else {
                            Paint-Cube $graphics $faces $horn $hornShadow $claw $outline
                        }
                    }
                    "tail" {
                        Paint-Cube $graphics $faces $scaleBase $scaleDark $scaleLight $outline
                        Add-TailStripe $graphics $faces.top $deepShadow
                        Add-TailStripe $graphics $faces.back $deepShadow
                    }
                    default {
                        Paint-Cube $graphics $faces $scaleBase $scaleDark $scaleLight $outline
                    }
                }
            }
        }

        if (Test-Path $texturePath) {
            Remove-Item -LiteralPath $texturePath -Force
        }

        if (Test-Path $glowmaskPath) {
            Remove-Item -LiteralPath $glowmaskPath -Force
        }

        $bitmap.Save($texturePath, [System.Drawing.Imaging.ImageFormat]::Png)
        $glowmask.Save($glowmaskPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $glowGraphics.Dispose()
    }
} finally {
    $bitmap.Dispose()
    $glowmask.Dispose()
}

Write-Output $outputGeoPath
Write-Output $texturePath
Write-Output $glowmaskPath
