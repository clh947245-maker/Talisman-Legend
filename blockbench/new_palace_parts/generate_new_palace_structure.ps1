$ErrorActionPreference = 'Stop'

$modelPath = Join-Path $PSScriptRoot 'new_palace_full.geo.json'
$outputPath = Join-Path $PSScriptRoot 'new_palace_structure.json'

$unitsPerBlock = 16.0
$epsilon = 0.0001
$rotatedSampleOffsets = @(0.2, 0.5, 0.8)

$palette = @(
    [ordered]@{ Key = 'smooth_sandstone'; Name = 'minecraft:smooth_sandstone' },
    [ordered]@{ Key = 'cut_sandstone'; Name = 'minecraft:cut_sandstone' },
    [ordered]@{ Key = 'sandstone'; Name = 'minecraft:sandstone' },
    [ordered]@{ Key = 'chiseled_sandstone'; Name = 'minecraft:chiseled_sandstone' },
    [ordered]@{ Key = 'red_nether_bricks'; Name = 'minecraft:red_nether_bricks' },
    [ordered]@{ Key = 'nether_bricks'; Name = 'minecraft:nether_bricks' },
    [ordered]@{ Key = 'gold_block'; Name = 'minecraft:gold_block' }
)

$paletteIndex = @{}
for ($i = 0; $i -lt $palette.Count; $i++) {
    $paletteIndex[$palette[$i].Key] = $i
}

function Rotate-AroundX {
    param([double[]]$Point, [double]$Radians)

    $cos = [Math]::Cos($Radians)
    $sin = [Math]::Sin($Radians)
    return [double[]]@(
        $Point[0],
        (($Point[1] * $cos) - ($Point[2] * $sin)),
        (($Point[1] * $sin) + ($Point[2] * $cos))
    )
}

function Rotate-AroundY {
    param([double[]]$Point, [double]$Radians)

    $cos = [Math]::Cos($Radians)
    $sin = [Math]::Sin($Radians)
    return [double[]]@(
        (($Point[0] * $cos) + ($Point[2] * $sin)),
        $Point[1],
        ((-$Point[0] * $sin) + ($Point[2] * $cos))
    )
}

function Rotate-AroundZ {
    param([double[]]$Point, [double]$Radians)

    $cos = [Math]::Cos($Radians)
    $sin = [Math]::Sin($Radians)
    return [double[]]@(
        (($Point[0] * $cos) - ($Point[1] * $sin)),
        (($Point[0] * $sin) + ($Point[1] * $cos)),
        $Point[2]
    )
}

function Rotate-Point {
    param([double[]]$Point, [double[]]$Pivot, [double[]]$Rotation)

    $translated = [double[]]@(
        ($Point[0] - $Pivot[0]),
        ($Point[1] - $Pivot[1]),
        ($Point[2] - $Pivot[2])
    )

    if ([Math]::Abs($Rotation[0]) -gt $epsilon) {
        $translated = Rotate-AroundX -Point $translated -Radians ($Rotation[0] * [Math]::PI / 180.0)
    }
    if ([Math]::Abs($Rotation[1]) -gt $epsilon) {
        $translated = Rotate-AroundY -Point $translated -Radians ($Rotation[1] * [Math]::PI / 180.0)
    }
    if ([Math]::Abs($Rotation[2]) -gt $epsilon) {
        $translated = Rotate-AroundZ -Point $translated -Radians ($Rotation[2] * [Math]::PI / 180.0)
    }

    return [double[]]@(
        ($translated[0] + $Pivot[0]),
        ($translated[1] + $Pivot[1]),
        ($translated[2] + $Pivot[2])
    )
}

function Inverse-RotatePoint {
    param([double[]]$Point, [double[]]$Pivot, [double[]]$Rotation)

    $translated = [double[]]@(
        ($Point[0] - $Pivot[0]),
        ($Point[1] - $Pivot[1]),
        ($Point[2] - $Pivot[2])
    )

    if ([Math]::Abs($Rotation[2]) -gt $epsilon) {
        $translated = Rotate-AroundZ -Point $translated -Radians ((-$Rotation[2]) * [Math]::PI / 180.0)
    }
    if ([Math]::Abs($Rotation[1]) -gt $epsilon) {
        $translated = Rotate-AroundY -Point $translated -Radians ((-$Rotation[1]) * [Math]::PI / 180.0)
    }
    if ([Math]::Abs($Rotation[0]) -gt $epsilon) {
        $translated = Rotate-AroundX -Point $translated -Radians ((-$Rotation[0]) * [Math]::PI / 180.0)
    }

    return [double[]]@(
        ($translated[0] + $Pivot[0]),
        ($translated[1] + $Pivot[1]),
        ($translated[2] + $Pivot[2])
    )
}

function Get-CubeVertices {
    param([double[]]$Origin, [double[]]$Size, [double[]]$Pivot, [double[]]$Rotation)

    $vertices = New-Object System.Collections.Generic.List[object]
    $xs = @($Origin[0], ($Origin[0] + $Size[0]))
    $ys = @($Origin[1], ($Origin[1] + $Size[1]))
    $zs = @($Origin[2], ($Origin[2] + $Size[2]))

    foreach ($x in $xs) {
        foreach ($y in $ys) {
            foreach ($z in $zs) {
                $point = [double[]]@($x, $y, $z)
                if ($null -ne $Rotation) {
                    $point = Rotate-Point -Point $point -Pivot $Pivot -Rotation $Rotation
                }
                $vertices.Add($point) | Out-Null
            }
        }
    }

    return $vertices
}

function Get-CubeAabb {
    param([double[]]$Origin, [double[]]$Size, [double[]]$Pivot, [double[]]$Rotation)

    $vertices = Get-CubeVertices -Origin $Origin -Size $Size -Pivot $Pivot -Rotation $Rotation
    $minX = [double]::PositiveInfinity
    $minY = [double]::PositiveInfinity
    $minZ = [double]::PositiveInfinity
    $maxX = [double]::NegativeInfinity
    $maxY = [double]::NegativeInfinity
    $maxZ = [double]::NegativeInfinity

    foreach ($vertex in $vertices) {
        $minX = [Math]::Min($minX, $vertex[0])
        $minY = [Math]::Min($minY, $vertex[1])
        $minZ = [Math]::Min($minZ, $vertex[2])
        $maxX = [Math]::Max($maxX, $vertex[0])
        $maxY = [Math]::Max($maxY, $vertex[1])
        $maxZ = [Math]::Max($maxZ, $vertex[2])
    }

    return [ordered]@{
        Min = [double[]]@($minX, $minY, $minZ)
        Max = [double[]]@($maxX, $maxY, $maxZ)
    }
}

function Test-PointInsideCube {
    param([double[]]$Point, [double[]]$Origin, [double[]]$Size, [double[]]$Pivot, [double[]]$Rotation)

    $localPoint = $Point
    if ($null -ne $Rotation) {
        $localPoint = Inverse-RotatePoint -Point $Point -Pivot $Pivot -Rotation $Rotation
    }

    return (
        $localPoint[0] -ge ($Origin[0] - $epsilon) -and
        $localPoint[0] -le (($Origin[0] + $Size[0]) + $epsilon) -and
        $localPoint[1] -ge ($Origin[1] - $epsilon) -and
        $localPoint[1] -le (($Origin[1] + $Size[1]) + $epsilon) -and
        $localPoint[2] -ge ($Origin[2] - $epsilon) -and
        $localPoint[2] -le (($Origin[2] + $Size[2]) + $epsilon)
    )
}

function Get-CubeMaterial {
    param([string]$BoneName, [double[]]$Origin, [double[]]$Size)

    switch ($BoneName) {
        'foundation_base' {
            if ($Origin[1] -lt 0) { return @{ Name = 'cut_sandstone'; Priority = 16 } }
            if ($Size[1] -le 16) { return @{ Name = 'smooth_sandstone'; Priority = 18 } }
            return @{ Name = 'cut_sandstone'; Priority = 20 }
        }
        'courtyard_floor' {
            if ($Size[1] -ge 12) { return @{ Name = 'chiseled_sandstone'; Priority = 28 } }
            return @{ Name = 'smooth_sandstone'; Priority = 24 }
        }
        'front_stairway' {
            return @{ Name = 'smooth_sandstone'; Priority = 22 }
        }
        'interior_layout' {
            if ($Size[1] -le 16) { return @{ Name = 'chiseled_sandstone'; Priority = 38 } }
            return @{ Name = 'sandstone'; Priority = 34 }
        }
        'grand_roof' {
            if ($Size[1] -ge 14 -or $Size[0] -le 36 -or $Size[2] -le 36) { return @{ Name = 'nether_bricks'; Priority = 58 } }
            return @{ Name = 'red_nether_bricks'; Priority = 52 }
        }
        'roof_accents' {
            if ($Size[0] -le 24 -and $Size[2] -le 24) { return @{ Name = 'gold_block'; Priority = 70 } }
            return @{ Name = 'nether_bricks'; Priority = 62 }
        }
        default {
            if ($Size[1] -le 16) { return @{ Name = 'chiseled_sandstone'; Priority = 30 } }
            if ($Size[0] -le 32 -and $Size[2] -le 32 -and $Size[1] -ge 96) { return @{ Name = 'sandstone'; Priority = 36 } }
            return @{ Name = 'cut_sandstone'; Priority = 32 }
        }
    }
}

$model = Get-Content -Raw $modelPath | ConvertFrom-Json
$bones = $model.'minecraft:geometry'[0].bones | Where-Object { $_.cubes }

$cubes = New-Object System.Collections.Generic.List[object]
$globalMin = [double[]]@([double]::PositiveInfinity, [double]::PositiveInfinity, [double]::PositiveInfinity)
$globalMax = [double[]]@([double]::NegativeInfinity, [double]::NegativeInfinity, [double]::NegativeInfinity)

foreach ($bone in $bones) {
    foreach ($cube in $bone.cubes) {
        $origin = [double[]]@([double]$cube.origin[0], [double]$cube.origin[1], [double]$cube.origin[2])
        $size = [double[]]@([double]$cube.size[0], [double]$cube.size[1], [double]$cube.size[2])
        $pivot = if ($null -ne $cube.pivot) { [double[]]@([double]$cube.pivot[0], [double]$cube.pivot[1], [double]$cube.pivot[2]) } else { $null }
        $rotation = if ($null -ne $cube.rotation) { [double[]]@([double]$cube.rotation[0], [double]$cube.rotation[1], [double]$cube.rotation[2]) } else { $null }
        $material = Get-CubeMaterial -BoneName $bone.name -Origin $origin -Size $size
        $aabb = Get-CubeAabb -Origin $origin -Size $size -Pivot $pivot -Rotation $rotation

        for ($axis = 0; $axis -lt 3; $axis++) {
            $globalMin[$axis] = [Math]::Min($globalMin[$axis], $aabb.Min[$axis])
            $globalMax[$axis] = [Math]::Max($globalMax[$axis], $aabb.Max[$axis])
        }

        $cubes.Add([ordered]@{
            BoneName = $bone.name
            Origin = $origin
            Size = $size
            Pivot = $pivot
            Rotation = $rotation
            Material = $material.Name
            Priority = $material.Priority
            Aabb = $aabb
        }) | Out-Null
    }
}

$sizeX = [int][Math]::Ceiling(($globalMax[0] - $globalMin[0]) / $unitsPerBlock)
$sizeY = [int][Math]::Ceiling(($globalMax[1] - $globalMin[1]) / $unitsPerBlock)
$sizeZ = [int][Math]::Ceiling(($globalMax[2] - $globalMin[2]) / $unitsPerBlock)

$occupied = @{}

function Set-OccupiedCell {
    param([int]$X, [int]$Y, [int]$Z, [string]$Material, [int]$Priority, [int]$Score, [string]$BoneName)

    $key = '{0},{1},{2}' -f $X, $Y, $Z
    if (-not $occupied.ContainsKey($key)) {
        $occupied[$key] = [ordered]@{
            x = $X
            y = $Y
            z = $Z
            material = $Material
            priority = $Priority
            score = $Score
            bone = $BoneName
        }
        return
    }

    $existing = $occupied[$key]
    if ($Priority -gt $existing.priority -or ($Priority -eq $existing.priority -and $Score -gt $existing.score)) {
        $occupied[$key] = [ordered]@{
            x = $X
            y = $Y
            z = $Z
            material = $Material
            priority = $Priority
            score = $Score
            bone = $BoneName
        }
    }
}

foreach ($cube in $cubes) {
    $cellMinX = [Math]::Max(0, [int][Math]::Floor(($cube.Aabb.Min[0] - $globalMin[0]) / $unitsPerBlock))
    $cellMinY = [Math]::Max(0, [int][Math]::Floor(($cube.Aabb.Min[1] - $globalMin[1]) / $unitsPerBlock))
    $cellMinZ = [Math]::Max(0, [int][Math]::Floor(($cube.Aabb.Min[2] - $globalMin[2]) / $unitsPerBlock))
    $cellMaxX = [Math]::Min($sizeX - 1, [int][Math]::Ceiling(($cube.Aabb.Max[0] - $globalMin[0]) / $unitsPerBlock))
    $cellMaxY = [Math]::Min($sizeY - 1, [int][Math]::Ceiling(($cube.Aabb.Max[1] - $globalMin[1]) / $unitsPerBlock))
    $cellMaxZ = [Math]::Min($sizeZ - 1, [int][Math]::Ceiling(($cube.Aabb.Max[2] - $globalMin[2]) / $unitsPerBlock))

    $isRotated = ($null -ne $cube.Rotation -and (
        [Math]::Abs($cube.Rotation[0]) -gt $epsilon -or
        [Math]::Abs($cube.Rotation[1]) -gt $epsilon -or
        [Math]::Abs($cube.Rotation[2]) -gt $epsilon
    ))

    for ($x = $cellMinX; $x -le $cellMaxX; $x++) {
        $worldMinX = $globalMin[0] + ($x * $unitsPerBlock)
        $worldMaxX = $worldMinX + $unitsPerBlock
        for ($y = $cellMinY; $y -le $cellMaxY; $y++) {
            $worldMinY = $globalMin[1] + ($y * $unitsPerBlock)
            $worldMaxY = $worldMinY + $unitsPerBlock
            for ($z = $cellMinZ; $z -le $cellMaxZ; $z++) {
                $worldMinZ = $globalMin[2] + ($z * $unitsPerBlock)
                $worldMaxZ = $worldMinZ + $unitsPerBlock

                if (-not $isRotated) {
                    $overlaps = (
                        $worldMinX -lt ($cube.Origin[0] + $cube.Size[0]) -and
                        $worldMaxX -gt $cube.Origin[0] -and
                        $worldMinY -lt ($cube.Origin[1] + $cube.Size[1]) -and
                        $worldMaxY -gt $cube.Origin[1] -and
                        $worldMinZ -lt ($cube.Origin[2] + $cube.Size[2]) -and
                        $worldMaxZ -gt $cube.Origin[2]
                    )

                    if ($overlaps) {
                        Set-OccupiedCell -X $x -Y $y -Z $z -Material $cube.Material -Priority $cube.Priority -Score 1 -BoneName $cube.BoneName
                    }

                    continue
                }

                $hitCount = 0
                foreach ($offsetX in $rotatedSampleOffsets) {
                    $sampleX = $worldMinX + ($offsetX * $unitsPerBlock)
                    foreach ($offsetY in $rotatedSampleOffsets) {
                        $sampleY = $worldMinY + ($offsetY * $unitsPerBlock)
                        foreach ($offsetZ in $rotatedSampleOffsets) {
                            $samplePoint = [double[]]@(
                                $sampleX,
                                $sampleY,
                                ($worldMinZ + ($offsetZ * $unitsPerBlock))
                            )
                            if (Test-PointInsideCube -Point $samplePoint -Origin $cube.Origin -Size $cube.Size -Pivot $cube.Pivot -Rotation $cube.Rotation) {
                                $hitCount++
                            }
                        }
                    }
                }

                if ($hitCount -gt 0) {
                    Set-OccupiedCell -X $x -Y $y -Z $z -Material $cube.Material -Priority $cube.Priority -Score $hitCount -BoneName $cube.BoneName
                }
            }
        }
    }
}

$blocks = $occupied.Values |
    Sort-Object y, z, x |
    ForEach-Object {
        [ordered]@{
            pos = @($_.x, $_.y, $_.z)
            state = $paletteIndex[$_.material]
        }
    }

$counts = [ordered]@{}
foreach ($entry in $occupied.Values) {
    if (-not $counts.Contains($entry.material)) {
        $counts[$entry.material] = 0
    }
    $counts[$entry.material]++
}

$output = [ordered]@{
    format = 'minecraft_structure_template_json'
    minecraft_version = '1.21.1'
    source_model = 'new_palace_full.geo.json'
    generated_at = (Get-Date).ToString('yyyy-MM-ddTHH:mm:sszzz')
    units_per_block = [int]$unitsPerBlock
    origin_model_space = @(
        [Math]::Round($globalMin[0], 3),
        [Math]::Round($globalMin[1], 3),
        [Math]::Round($globalMin[2], 3)
    )
    size = @($sizeX, $sizeY, $sizeZ)
    palette = @(
        foreach ($entry in $palette) {
            $outEntry = [ordered]@{
                Name = $entry.Name
            }
            if ($entry.Contains('Properties')) {
                $outEntry.Properties = $entry.Properties
            }
            $outEntry
        }
    )
    blocks = $blocks
    entities = @()
    block_counts = $counts
    notes = @(
        'This structure template approximates the new imperial palace as Minecraft block occupancy.',
        'The palette emphasizes a monumental sandstone body, red nether-brick roof masses, and gilded peak accents.',
        'The palace is intentionally hollowed into multiple halls, wings, corridors, and an elevated rear keep.'
    )
}

$json = $output | ConvertTo-Json -Depth 100
[System.IO.File]::WriteAllText($outputPath, $json)

Write-Output "Generated structure json: $outputPath"
Write-Output "Size: $sizeX x $sizeY x $sizeZ"
Write-Output "Blocks: $($blocks.Count)"
