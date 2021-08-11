package tanks.hotbar.item;

import tanks.Game;
import tanks.Panel;
import tanks.Player;
import tanks.bullet.*;
import tanks.hotbar.item.property.ItemPropertyBoolean;
import tanks.hotbar.item.property.ItemPropertyDouble;
import tanks.hotbar.item.property.ItemPropertyInt;
import tanks.hotbar.item.property.ItemPropertySelector;
import tanks.tank.Tank;
import tanks.tank.TankPlayer;
import tanks.tank.TankPlayerRemote;

import java.util.HashMap;

public class ItemBullet extends Item
{
	public static final String item_name = "bullet";

	public Bullet.BulletEffect effect = Bullet.BulletEffect.none;
	public double speed = 25.0 / 8;
	public int bounces = 1;
	public double damage = 1;
	public int maxAmount = 5;
	public double cooldown = 20;
	public double size = Bullet.bullet_size;
	public double recoil = 1.0;
	public boolean heavy = false;

	public double fractionUsed = 0;

	public int liveBullets;

	public String name;
	
	public String className;

	public Class<? extends Bullet> bulletClass = Bullet.class;

	public static HashMap<String, Bullet.BulletEffect> effectsMap1 = new HashMap<>();
	public static HashMap<Bullet.BulletEffect, String> effectsMap2 = new HashMap<>();

	public static HashMap<String, Class<? extends Bullet>> classMap1 = new HashMap<>();
	public static HashMap<Class<? extends Bullet>, String> classMap2 = new HashMap<>();

	public ItemBullet(Player p)
	{
		super(p);
		this.rightClick = false;
		this.isConsumable = true;

		new ItemPropertySelector(this.properties, "type", Game.registryBullet.getEntryNames(), Game.registryBullet.getImageNames(), 0);
		new ItemPropertySelector(this.properties, "effect", new String[]{"none", "trail", "fire", "fire_and_smoke", "dark_fire", "ice", "ember"},
				new String[]{"bullet_large.png", "bullet_normal.png", "bullet_fire.png", "bullet_fire_trail.png", "bullet_dark_fire.png", "bullet_freeze.png", "bullet_boost.png"}, 0);
		new ItemPropertyDouble(this.properties, "speed", 3.125);
		new ItemPropertyInt(this.properties, "bounces", 1);
		new ItemPropertyDouble(this.properties, "damage", 1.0);
		new ItemPropertyInt(this.properties, "max_live_bullets", 5);
		new ItemPropertyDouble(this.properties, "cooldown", 20.0);
		new ItemPropertyDouble(this.properties, "size", 10.0);
		new ItemPropertyDouble(this.properties, "recoil", 1.0);
		new ItemPropertyBoolean(this.properties, "heavy", false);
	}

	public ItemBullet()
	{
		this(null);
	}

	@Override
	public void use()
	{
		try
		{
			Tank m = this.getUser();

			Bullet b = bulletClass.getConstructor(Double.class, Double.class, Integer.class, Tank.class, ItemBullet.class).newInstance(m.posX, m.posY, bounces, (Tank) m, this);

			b.damage = this.damage;
			b.effect = this.effect;
			b.size = this.size;
			b.heavy = heavy;
			b.recoil = recoil;

			if (this.cooldown <= 0)
			{
				b.frameDamageMultipler = Panel.frameFrequency;
				this.fractionUsed += Panel.frameFrequency;
			}
			else
				this.fractionUsed++;

			m.cooldown = this.cooldown;

			if (m instanceof TankPlayerRemote)
				((TankPlayerRemote) m).fireBullet(b, speed);
			else if (m instanceof TankPlayer)
				((TankPlayer) m).fireBullet(b, speed);

			while (this.fractionUsed >= 1)
			{
				this.stackSize--;
				this.fractionUsed--;
			}

			if (this.stackSize <= 0)
				this.destroy = true;
		}
		catch (Exception e)
		{
			Game.exitToCrash(e);
		}
	}

	@Override
	public boolean usable()
	{
		Tank t = this.getUser();
		return t != null && (this.maxAmount <= 0 || this.liveBullets < this.maxAmount) && !(t.cooldown > 0) && this.stackSize > 0;
	}

	@Override
	public String toString()
	{
		return super.toString() + "," + item_name + ","
				+ className + "," + effectsMap2.get(effect) + "," + speed + "," + bounces + "," + damage + "," + maxAmount + "," + cooldown + "," + size + "," + recoil + "," + heavy;
	}

	@Override
	public void fromString(String s)
	{
		String[] p = s.split(",");

		this.bulletClass = classMap1.get(p[0]);
		this.className = p[0];
		this.effect = effectsMap1.get(p[1]);

		this.speed = Double.parseDouble(p[2]);
		this.bounces = Integer.parseInt(p[3]);
		this.damage = Double.parseDouble(p[4]);
		this.maxAmount = Integer.parseInt(p[5]);
		this.cooldown = Double.parseDouble(p[6]);
		this.size = Double.parseDouble(p[7]);
		this.recoil = Double.parseDouble(p[8]);
		this.heavy = Boolean.parseBoolean(p[9]);
	}

	@Override
	public void importProperties()
	{
		super.importProperties();

		this.setProperty("type", this.className);
		this.setProperty("effect", effectsMap2.get(this.effect));
		this.setProperty("speed", this.speed);
		this.setProperty("bounces", this.bounces);
		this.setProperty("damage", this.damage);
		this.setProperty("max_live_bullets", this.maxAmount);
		this.setProperty("cooldown", this.cooldown);
		this.setProperty("size", this.size);
		this.setProperty("recoil", this.recoil);
		this.setProperty("heavy", this.heavy);
	}

	@Override
	public String getTypeName()
	{
		return "Bullet";
	}

	@Override
	public void exportProperties()
	{
		super.exportProperties();

		this.className = (String) this.getProperty("type");
		this.bulletClass = classMap1.get(this.className);
		this.effect = effectsMap1.get(this.getProperty("effect"));
		this.speed = (double) this.getProperty("speed");
		this.bounces = (int) this.getProperty("bounces");
		this.damage = (double) this.getProperty("damage");
		this.maxAmount = (int) this.getProperty("max_live_bullets");
		this.cooldown = (double) this.getProperty("cooldown");
		this.size = (double) this.getProperty("size");
		this.recoil = (double) this.getProperty("recoil");
		this.heavy = (boolean) this.getProperty("heavy");
	}

	public double getRange()
	{
		if (BulletArc.class.isAssignableFrom(this.bulletClass))
			return this.speed / 3.125 * 1000.0;
		else
			return -1;
	}

	public static void initializeMaps()
	{
		addToMaps(effectsMap1, effectsMap2, "none", Bullet.BulletEffect.none);
		addToMaps(effectsMap1, effectsMap2, "trail", Bullet.BulletEffect.trail);
		addToMaps(effectsMap1, effectsMap2, "fire", Bullet.BulletEffect.fire);
		addToMaps(effectsMap1, effectsMap2, "fire_and_smoke", Bullet.BulletEffect.fireTrail);
		addToMaps(effectsMap1, effectsMap2, "dark_fire", Bullet.BulletEffect.darkFire);
		addToMaps(effectsMap1, effectsMap2, "ice", Bullet.BulletEffect.ice);
		addToMaps(effectsMap1, effectsMap2, "ember", Bullet.BulletEffect.ember);
	}

	public static <X, Y> void addToMaps(HashMap<X, Y> map1, HashMap<Y, X> map2, X a, Y b)
	{
		map1.put(a, b);
		map2.put(b, a);
	}
}
