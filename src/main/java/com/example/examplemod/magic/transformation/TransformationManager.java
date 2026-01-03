package com.example.examplemod.magic.transformation;

import com.example.examplemod.magic.transformation.animal.ChickenTransformation;
import com.example.examplemod.magic.transformation.animal.SheepTransformation;
import com.example.examplemod.magic.transformation.animal.CowTransformation;
import com.example.examplemod.magic.transformation.animal.PigTransformation;
import com.example.examplemod.magic.transformation.animal.HorseTransformation;
import com.example.examplemod.magic.transformation.animal.WolfTransformation;
import com.example.examplemod.magic.transformation.animal.CatTransformation;
import com.example.examplemod.magic.transformation.animal.AllayTransformation;
import com.example.examplemod.magic.transformation.animal.BatTransformation;
import com.example.examplemod.magic.transformation.animal.ParrotTransformation;
import com.example.examplemod.magic.transformation.animal.BeeTransformation;
import com.example.examplemod.magic.transformation.animal.CodTransformation;
import com.example.examplemod.magic.transformation.animal.DolphinTransformation;
import com.example.examplemod.magic.transformation.animal.TurtleTransformation;
import com.example.examplemod.magic.transformation.animal.TadpoleTransformation;
import com.example.examplemod.magic.transformation.animal.FrogTransformation;
import com.example.examplemod.magic.transformation.animal.SquidTransformation;
import com.example.examplemod.magic.transformation.animal.AxolotlTransformation;
import com.example.examplemod.magic.transformation.animal.PolarBearTransformation;
import com.example.examplemod.magic.transformation.animal.MuleTransformation;
import com.example.examplemod.magic.transformation.animal.DonkeyTransformation;
import com.example.examplemod.magic.transformation.animal.FoxTransformation;
import com.example.examplemod.magic.transformation.animal.SnifferTransformation;
import com.example.examplemod.magic.transformation.animal.LlamaTransformation;
import com.example.examplemod.magic.transformation.animal.RabbitTransformation;
import com.example.examplemod.magic.transformation.animal.PandaTransformation;
import com.example.examplemod.magic.transformation.animal.PufferfishTransformation;

import java.util.HashMap;
import java.util.Map;

/**
 * 变身管理器
 * <p>
 * 负责注册和检索所有的变身形态。
 * </p>
 */
public class TransformationManager {

    private static final Map<Integer, ITransformation> REGISTRY = new HashMap<>();

    // ID 常量
    public static final int ID_REVERT = 0;
    public static final int ID_CHICKEN = 1;
    public static final int ID_SHEEP = 2;
    public static final int ID_COW = 3;
    public static final int ID_PIG = 4;
    public static final int ID_HORSE = 5;
    public static final int ID_WOLF = 6;
    public static final int ID_CAT = 7;
    public static final int ID_ALLAY = 8;
    public static final int ID_BAT = 9;
    public static final int ID_PARROT = 10;
    public static final int ID_BEE = 11;
    public static final int ID_COD = 12;
    public static final int ID_DOLPHIN = 13;
    public static final int ID_TURTLE = 14;
    public static final int ID_TADPOLE = 15;
    public static final int ID_FROG = 16;
    public static final int ID_SQUID = 17;
    public static final int ID_AXOLOTL = 18;
    public static final int ID_POLAR_BEAR = 19;
    public static final int ID_MULE = 20;
    public static final int ID_DONKEY = 21;
    public static final int ID_FOX = 22;
    public static final int ID_SNIFFER = 23;
    public static final int ID_LLAMA = 24;
    public static final int ID_RABBIT = 25;
    public static final int ID_PANDA = 26;
    public static final int ID_PUFFERFISH = 27;

    static {
        register(ID_REVERT, new RevertTransformation());
        register(ID_CHICKEN, new ChickenTransformation());
        register(ID_SHEEP, new SheepTransformation());
        register(ID_COW, new CowTransformation());
        register(ID_PIG, new PigTransformation());
        register(ID_HORSE, new HorseTransformation());
        register(ID_WOLF, new WolfTransformation());
        register(ID_CAT, new CatTransformation());
        register(ID_ALLAY, new AllayTransformation());
        register(ID_BAT, new BatTransformation());
        register(ID_PARROT, new ParrotTransformation());
        register(ID_BEE, new BeeTransformation());
        register(ID_COD, new CodTransformation());
        register(ID_DOLPHIN, new DolphinTransformation());
        register(ID_TURTLE, new TurtleTransformation());
        register(ID_TADPOLE, new TadpoleTransformation());
        register(ID_FROG, new FrogTransformation());
        register(ID_SQUID, new SquidTransformation());
        register(ID_AXOLOTL, new AxolotlTransformation());
        register(ID_POLAR_BEAR, new PolarBearTransformation());
        register(ID_MULE, new MuleTransformation());
        register(ID_DONKEY, new DonkeyTransformation());
        register(ID_FOX, new FoxTransformation());
        register(ID_SNIFFER, new SnifferTransformation());
        register(ID_LLAMA, new LlamaTransformation());
        register(ID_RABBIT, new RabbitTransformation());
        register(ID_PANDA, new PandaTransformation());
        register(ID_PUFFERFISH, new PufferfishTransformation());
    }

    public static void register(int id, ITransformation transformation) {
        REGISTRY.put(id, transformation);
    }

    public static ITransformation getTransformation(int id) {
        return REGISTRY.get(id);
    }

    public static int getTransformationCount() {
        return REGISTRY.size();
    }
}
