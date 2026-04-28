from __future__ import annotations

import math
import os
import struct
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "promo_videos"
WIDTH, HEIGHT = 1280, 720
FPS = 10
SCENE_SECONDS = 4

FONT_REGULAR = Path(r"C:\Windows\Fonts\simhei.ttf")
FONT_BOLD = Path(r"C:\Windows\Fonts\Dengb.ttf")


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    path = FONT_BOLD if bold and FONT_BOLD.exists() else FONT_REGULAR
    return ImageFont.truetype(str(path), size=size)


TITLE = font(58, True)
SUBTITLE = font(32)
BODY = font(30)
SMALL = font(24)
TAG = font(22, True)


def load_rgba(path: str) -> Image.Image:
    return Image.open(ROOT / path).convert("RGBA")


PROMO_MAGIC = load_rgba("promo_images/chen_mod_magic_promo_992x558.png")
PROMO_TALISMANS = load_rgba("promo_images/twelve_talismans_promo.png")

ICON_PATHS = [
    "src/main/resources/assets/chen_mod/textures/item/talisman/horse_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/ox_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/rabbit_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/snack_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/dog_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/rooster_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/monkey_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/tiger_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/dragon_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/mouse_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/pig_talisman.png",
    "src/main/resources/assets/chen_mod/textures/item/talisman/sheep_talisman.png",
]
ICONS = [load_rgba(path) for path in ICON_PATHS]
ONI_MASK = load_rgba("src/main/resources/assets/chen_mod/textures/item/mask/oni_mask.png")
PUFFER = load_rgba("src/main/resources/assets/chen_mod/textures/item/weapon/pufferfish_weapon.png")
SHADOW = load_rgba("src/main/resources/assets/chen_mod/textures/entity/shadow_ninja.png")
DRAGON = load_rgba("src/main/resources/assets/chen_mod/textures/entity/dragon_brutel.png")


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def ease(t: float) -> float:
    return 0.5 - math.cos(max(0.0, min(1.0, t)) * math.pi) * 0.5


def cover(img: Image.Image, w: int, h: int, scale_extra: float = 0.0) -> Image.Image:
    scale = max(w / img.width, h / img.height) * (1.0 + scale_extra)
    size = (math.ceil(img.width * scale), math.ceil(img.height * scale))
    return img.resize(size, Image.Resampling.LANCZOS)


def paste_center(base: Image.Image, img: Image.Image, cx: int, cy: int) -> None:
    base.alpha_composite(img, (int(cx - img.width / 2), int(cy - img.height / 2)))


def text(draw: ImageDraw.ImageDraw, xy: tuple[int, int], value: str, fnt, fill=(255, 255, 255, 255), anchor=None) -> None:
    x, y = xy
    for dx, dy in [(-2, 2), (2, 2), (0, 3)]:
        draw.text((x + dx, y + dy), value, font=fnt, fill=(0, 0, 0, 150), anchor=anchor)
    draw.text(xy, value, font=fnt, fill=fill, anchor=anchor)


def rounded_panel(base: Image.Image, box: tuple[int, int, int, int], fill=(12, 14, 18, 188), outline=(255, 205, 91, 120)) -> None:
    overlay = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    d.rounded_rectangle(box, radius=18, fill=fill, outline=outline, width=2)
    base.alpha_composite(overlay)


def draw_tags(draw: ImageDraw.ImageDraw, labels: list[str], y: int) -> None:
    x = 64
    for label in labels:
        bbox = draw.textbbox((0, 0), label, font=TAG)
        w = bbox[2] - bbox[0] + 34
        draw.rounded_rectangle((x, y, x + w, y + 40), radius=20, fill=(255, 205, 91, 230))
        draw.text((x + 17, y + 8), label, font=TAG, fill=(31, 25, 14, 255))
        x += w + 14


def base_gradient() -> Image.Image:
    img = Image.new("RGBA", (WIDTH, HEIGHT))
    px = img.load()
    for y in range(HEIGHT):
        for x in range(WIDTH):
            gx = x / WIDTH
            gy = y / HEIGHT
            r = int(22 + 42 * gx + 22 * (1 - gy))
            g = int(20 + 34 * gy)
            b = int(28 + 28 * (1 - gx) + 16 * gy)
            px[x, y] = (r, g, b, 255)
    return img


def vignette(img: Image.Image) -> Image.Image:
    mask = Image.new("L", (WIDTH, HEIGHT), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((-220, -120, WIDTH + 220, HEIGHT + 180), fill=210)
    mask = mask.filter(ImageFilter.GaussianBlur(85))
    dark = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 155))
    img = Image.composite(img, dark, mask)
    return img


def title_slide(t: float) -> Image.Image:
    bg = cover(PROMO_MAGIC, WIDTH, HEIGHT, 0.08 * ease(t))
    x = int((WIDTH - bg.width) / 2 + lerp(-22, 22, t))
    y = int((HEIGHT - bg.height) / 2)
    img = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 255))
    img.alpha_composite(bg, (x, y))
    img = vignette(img)
    panel = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    d = ImageDraw.Draw(panel)
    d.rectangle((0, 0, WIDTH, HEIGHT), fill=(0, 0, 0, 58))
    img.alpha_composite(panel)
    d = ImageDraw.Draw(img)
    text(d, (70, 210), "成龙历险记模组", TITLE, (255, 233, 166, 255))
    text(d, (73, 286), "十二符咒、黑影兵团与圣主宫殿", SUBTITLE)
    draw_tags(d, ["NeoForge 1.21.0", "中文魔法冒险", "探索 / 收集 / 战斗"], 365)
    return img


def talisman_slide(t: float) -> Image.Image:
    img = base_gradient()
    poster = cover(PROMO_TALISMANS, 710, 400, 0.02 * ease(t))
    poster = poster.crop(((poster.width - 710) // 2, (poster.height - 400) // 2, (poster.width + 710) // 2, (poster.height + 400) // 2))
    poster = poster.filter(ImageFilter.UnsharpMask(radius=1, percent=110))
    rounded_panel(img, (48, 70, 770, 492), (8, 10, 14, 170), (255, 205, 91, 160))
    img.alpha_composite(poster, (54, 81))
    d = ImageDraw.Draw(img)
    text(d, (820, 110), "十二符咒", TITLE, (255, 230, 158, 255))
    lines = ["马：驱除异常", "兔：高速移动", "狗：濒死守护", "猴：七十二变", "虎：阴阳分身", "羊：灵魂出窍"]
    for i, line in enumerate(lines):
        text(d, (835, 205 + i * 52), line, BODY)
    for i, icon in enumerate(ICONS):
        s = 62 + int(8 * math.sin(t * math.pi * 2 + i))
        tile = icon.resize((s, s), Image.Resampling.NEAREST)
        col, row = i % 6, i // 6
        paste_center(img, tile, 150 + col * 170, 580 + row * 70)
    return img


def combat_slide(t: float) -> Image.Image:
    img = base_gradient()
    d = ImageDraw.Draw(img)
    text(d, (64, 78), "战斗能力全面升级", TITLE, (255, 230, 158, 255))
    cards = [
        ("龙符咒", "发射龙爆破，命中后爆炸", ICONS[8]),
        ("猪符咒", "双眼激光灼伤路径敌人", ICONS[10]),
        ("鼠符咒", "光束命中方块或生物后停下", ICONS[9]),
        ("老爹的河豚", "攻击与感应魔气双模式", PUFFER),
    ]
    for i, (name, desc, icon) in enumerate(cards):
        x = 78 + (i % 2) * 575
        y = 190 + (i // 2) * 205
        rounded_panel(img, (x, y, x + 500, y + 148), (18, 22, 27, 210), (255, 205, 91, 130))
        bob = int(6 * math.sin(t * math.pi * 2 + i))
        tile = icon.resize((88, 88), Image.Resampling.NEAREST)
        paste_center(img, tile, x + 72, y + 74 + bob)
        text(d, (x + 140, y + 32), name, SUBTITLE, (255, 236, 184, 255))
        text(d, (x + 140, y + 86), desc, SMALL)
    return img


def transform_slide(t: float) -> Image.Image:
    img = base_gradient()
    d = ImageDraw.Draw(img)
    text(d, (64, 78), "变身、灵魂与分身", TITLE, (255, 230, 158, 255))
    text(d, (70, 166), "猴符咒按住 TAB + 滚轮选择形态，动物能力不再只是装饰。", BODY)
    text(d, (70, 218), "羊符咒让意识离开身体探索，虎符咒分离善恶并追踪分身。", BODY)
    labels = ["鸡", "牛", "猪", "狼", "猫", "蜜蜂", "海豚", "熊猫", "河豚", "悦灵", "狐狸", "嗅探兽"]
    cx, cy = WIDTH // 2, 470
    for i, label in enumerate(labels):
        angle = t * math.tau + i / len(labels) * math.tau
        r = 205
        x = int(cx + math.cos(angle) * r)
        y = int(cy + math.sin(angle) * r * 0.52)
        d.ellipse((x - 48, y - 32, x + 48, y + 32), fill=(255, 205, 91, 220), outline=(255, 247, 211, 180), width=2)
        d.text((x, y - 15), label, font=TAG, fill=(31, 25, 14, 255), anchor="ma")
    rounded_panel(img, (464, 382, 816, 556), (10, 14, 20, 220), (255, 205, 91, 160))
    text(d, (640, 420), "猴符咒", SUBTITLE, (255, 236, 184, 255), anchor="ma")
    text(d, (640, 472), "七十二变", TITLE, (255, 255, 255, 255), anchor="ma")
    return img


def shadow_slide(t: float) -> Image.Image:
    img = base_gradient()
    d = ImageDraw.Draw(img)
    text(d, (64, 78), "黑影兵团与圣主", TITLE, (255, 230, 158, 255))
    text(d, (70, 158), "戴上忍者面具，召唤暗影兵团；深入世界，寻找圣主宫殿。", BODY)
    mask = ONI_MASK.resize((132, 132), Image.Resampling.NEAREST)
    dragon = DRAGON.resize((270, 270), Image.Resampling.NEAREST)
    ninja = SHADOW.resize((210, 210), Image.Resampling.NEAREST)
    paste_center(img, mask, 180, 420)
    paste_center(img, ninja, 530 + int(12 * math.sin(t * math.tau)), 430)
    paste_center(img, dragon, 940, 410 + int(10 * math.cos(t * math.tau)))
    text(d, (100, 555), "忍者面具", SUBTITLE, (255, 236, 184, 255))
    text(d, (440, 555), "暗影忍者", SUBTITLE, (255, 236, 184, 255))
    text(d, (875, 555), "圣主降临", SUBTITLE, (255, 236, 184, 255))
    return img


def ending_slide(t: float) -> Image.Image:
    bg = cover(PROMO_MAGIC, WIDTH, HEIGHT, 0.04)
    x = int((WIDTH - bg.width) / 2)
    y = int((HEIGHT - bg.height) / 2)
    img = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 255))
    img.alpha_composite(bg, (x, y))
    img = vignette(img)
    d = ImageDraw.Draw(img)
    text(d, (640, 235), "探索世界，收集符咒", TITLE, (255, 230, 158, 255), anchor="ma")
    text(d, (640, 320), "召唤黑影兵团，挑战圣主魔法", SUBTITLE, anchor="ma")
    text(d, (640, 430), "要用魔法打败魔法", TITLE, (255, 255, 255, 255), anchor="ma")
    alpha = int(255 * ease(t))
    overlay = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, max(0, alpha - 210)))
    img.alpha_composite(overlay)
    return img


SCENES = [title_slide, talisman_slide, combat_slide, transform_slide, shadow_slide, ending_slide]


def make_frames() -> list[Image.Image]:
    frames: list[Image.Image] = []
    frames_per_scene = FPS * SCENE_SECONDS
    for scene in SCENES:
        for frame in range(frames_per_scene):
            t = frame / (frames_per_scene - 1)
            img = scene(t)
            fade = min(1.0, frame / 7, (frames_per_scene - 1 - frame) / 7)
            if fade < 1.0:
                black = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, int(255 * (1 - fade))))
                img.alpha_composite(black)
            frames.append(img.convert("RGB"))
    return frames


def write_avi_mjpeg(path: Path, frames: list[Image.Image]) -> None:
    jpeg_chunks: list[bytes] = []
    for img in frames:
        import io

        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=88, subsampling=1, optimize=False)
        data = buf.getvalue()
        if len(data) % 2:
            data += b"\0"
        jpeg_chunks.append(data)

    def chunk(tag: bytes, payload: bytes) -> bytes:
        return tag + struct.pack("<I", len(payload)) + payload + (b"\0" if len(payload) % 2 else b"")

    total_frames = len(frames)
    max_bytes = max(len(c) for c in jpeg_chunks)
    us_per_frame = int(1_000_000 / FPS)
    avih = struct.pack(
        "<IIIIIIIIIIIIII",
        us_per_frame,
        max_bytes * FPS,
        0,
        0x10,
        total_frames,
        0,
        1,
        max_bytes,
        WIDTH,
        HEIGHT,
        0,
        0,
        0,
        0,
    )
    strh = struct.pack(
        "<4s4sIIIIIIIIIIII",
        b"vids",
        b"MJPG",
        0,
        0,
        0,
        1,
        FPS,
        0,
        total_frames,
        max_bytes,
        0xFFFFFFFF,
        0,
        0,
        0,
    ) + struct.pack("<hhhh", 0, 0, WIDTH, HEIGHT)
    strf = struct.pack("<IIIHH4sIiiII", 40, WIDTH, HEIGHT, 1, 24, b"MJPG", WIDTH * HEIGHT * 3, 0, 0, 0, 0)
    strl = b"LIST" + struct.pack("<I", 4 + len(chunk(b"strh", strh)) + len(chunk(b"strf", strf))) + b"strl" + chunk(b"strh", strh) + chunk(b"strf", strf)
    hdrl_payload = chunk(b"avih", avih) + strl
    hdrl = b"LIST" + struct.pack("<I", 4 + len(hdrl_payload)) + b"hdrl" + hdrl_payload

    movi_payload = bytearray()
    idx_entries = []
    offset = 4
    for data in jpeg_chunks:
        idx_entries.append((b"00dc", 0x10, offset, len(data)))
        movi_payload.extend(chunk(b"00dc", data))
        offset += 8 + len(data)
    movi = b"LIST" + struct.pack("<I", 4 + len(movi_payload)) + b"movi" + bytes(movi_payload)
    idx = b"".join(struct.pack("<4sIII", tag, flags, off, size) for tag, flags, off, size in idx_entries)
    body = b"AVI " + hdrl + movi + chunk(b"idx1", idx)
    path.write_bytes(b"RIFF" + struct.pack("<I", len(body)) + body)


def main() -> None:
    OUT_DIR.mkdir(exist_ok=True)
    frames = make_frames()
    avi_path = OUT_DIR / "chen_mod_intro_cn.avi"
    gif_path = OUT_DIR / "chen_mod_intro_cn.gif"
    write_avi_mjpeg(avi_path, frames)
    gif_frames = [f.resize((640, 360), Image.Resampling.LANCZOS).convert("P", palette=Image.Palette.ADAPTIVE) for f in frames]
    gif_frames[0].save(gif_path, save_all=True, append_images=gif_frames[1:], duration=int(1000 / FPS), loop=0, optimize=True)
    print(f"Wrote {avi_path}")
    print(f"Wrote {gif_path}")


if __name__ == "__main__":
    main()
