package tanks;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import tanks.tank.Tank;
import tanks.tank.TankUnknown;

public class RegistryTank 
{
	public ArrayList<TankEntry> tankEntries = new ArrayList<TankEntry>();
	protected double maxTankWeight = 0;
	
	public static void loadRegistry(String homedir) 
	{
		Game.registryTank.tankEntries.clear();
		Game.registryTank.maxTankWeight = 0;
		
		String path = homedir + Game.tankRegistryPath;
		try 
		{
			Scanner in = new Scanner(new File(path));
			while (in.hasNextLine()) 
			{
				String line = in.nextLine();
				String[] tankLine = line.split(",");
				
				if (tankLine[0].charAt(0) == '#') 
				{ 
					continue; 
				}
				if (tankLine[2].toLowerCase().equals("default")) 
				{
					boolean foundTank = false;
					for (int i = 0; i < Game.defaultTanks.size(); i++)
					{
						if (tankLine[0].equals(Game.defaultTanks.get(i).name))
						{
							Game.defaultTanks.get(i).registerEntry(Game.registryTank, Double.parseDouble(tankLine[1]));
							foundTank = true;
							break;
						}
					}
					
					if (!foundTank)
						Game.logger.println (new Date().toString() + " (syswarn) the default tank '" + tankLine[0] + "' does not exist!");
				}
				else 
				{
					try 
					{
						ClassLoader classLoader = RegistryTank.class.getClassLoader();
						Class<?> clazz = classLoader.getClass();
						
						if (!clazz.equals(URLClassLoader.class)) // Java 9 jdk.internal.loader.ClassLoaders$AppClassLoader
							clazz = clazz.getSuperclass(); // jdk.internal.loader.BuiltinClassLoader
						
						Field field = clazz.getDeclaredField("ucp");
						field.setAccessible(true);
						Object ucp = field.get(classLoader);
						
						Class<?> URLClassPath = ucp.getClass();
						Method method = URLClassPath.getDeclaredMethod("addURL", URL.class);
						method.invoke(ucp, new File(tankLine[3]).toURI().toURL());
						
						Class<? extends Tank> clasz = Class.forName(tankLine[4], true, classLoader).asSubclass(Tank.class);
						new RegistryTank.TankEntry(Game.registryTank, clasz, tankLine[0], Double.parseDouble(tankLine[1]));
					}
					catch (Exception e) 
					{
						e.printStackTrace();
						Game.logger.println(new Date().toString() + " (syswarn) error loading custom tank '" + tankLine[0] + "'. try adding the path to your jvm classpath. ignoring.");
					}
				}
			}
			in.close();
		} 
		catch (Exception e)
		{
			Game.logger.println (new Date().toString() + " (syswarn) tank registry file is nonexistent or broken, using default:");
			e.printStackTrace(Game.logger);
			
			for (int i = 0; i < Game.defaultTanks.size(); i++)
			{
				Game.defaultTanks.get(i).registerEntry(Game.registryTank);
			}
		}
	}
	
	public static void initRegistry(String homedir) 
	{
		String path = homedir + Game.tankRegistryPath;
		try 
		{
			new File(path).createNewFile();
		}
		catch (IOException e) 
		{
			Game.logger.println (new Date().toString() + " (syserr) file permissions are broken! cannot initialize tank registry.");
			System.exit(1);
		}
		try 
		{
			PrintStream writer = new PrintStream(new File(path));
			writer.println("# This is the Tank Registry file!");
			writer.println("# A registry entry is a line in the file");
			writer.println("# The parameters are name, rarity, custom/default, jar location, and class");
			writer.println("# Built in tanks do not use the last 2 parameters");
			writer.println("# and have 'default' written for the third parameter");
			writer.println("# To make a custom tank, import the 'Tanks' jar into a java project,");
			writer.println("# write a class extending Tank or EnemyTank, and export as a jar file.");
			writer.println("# To import a custom tank, put the jar file somewhere on your computer,");
			writer.println("# put 'custom' for parameter 3");
			writer.println("# and put its absolute file path as parameter 4 in this file.");
			writer.println("# Then, put a comma and write the Class name with package and all as parameter 5.");
			writer.println("# Example custom tank entry: 'mytank,1,custom,C:\\Users\\potato\\.tanks.d\\MyTank.jar,com.potato.MyTank'");
			writer.println("# Don't leave any blank lines!");
			
			for (int i = 0; i < Game.defaultTanks.size(); i++)
			{
				writer.println(Game.defaultTanks.get(i).getString());
			}
		} 
		catch (Exception e)
		{
			Game.logger.println(new Date().toString() + " (syserr) something broke! could not initialize tank registry:");
			e.printStackTrace(Game.logger);
			System.exit(1);
		}
		
	}
	
	static class TankEntry
	{
		public final Class<? extends Tank> tank;
		public final String name;
		public final double weight;
	
		protected double startWeight;
		protected double endWeight;
		
		public TankEntry(RegistryTank r, Class<? extends Tank> tank, String name, double weight)
		{
			this.tank = tank;
			this.name = name;
			this.weight = weight;
	
			this.startWeight = r.maxTankWeight;
			r.maxTankWeight += weight;
			this.endWeight = r.maxTankWeight;
			
			r.tankEntries.add(this);
		}
		
		protected TankEntry()
		{
			this.tank = TankUnknown.class;
			this.name = "unknown";
			this.weight = 0;
		}
		
		protected TankEntry(String name)
		{
			this.tank = TankUnknown.class;
			this.name = name;
			this.weight = 0;
		}
		
		public Tank getTank(double x, double y, double a)
		{
			try 
			{
				return tank.getConstructor(String.class, double.class, double.class, double.class).newInstance(this.name, x, y, a);
			}
			catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) 
			{
				e.printStackTrace();
				return null;
			}
		}
		
		public static TankEntry getUnknownEntry()
		{
			return new TankEntry();
		}
		
		public static TankEntry getUnknownEntry(String name)
		{
			return new TankEntry(name);
		}
	}
	
	static class DefaultTankEntry
	{
		public final Class<? extends Tank> tank;
		public final String name;
		public final double weight;
	
		protected double startWeight;
		protected double endWeight;
		
		public DefaultTankEntry(Class<? extends Tank> tank, String name, double weight)
		{
			this.tank = tank;
			this.name = name;
			this.weight = weight;
		}
		
		public TankEntry registerEntry(RegistryTank r)
		{
			return new TankEntry(r, this.tank, this.name, this.weight);
		}
		
		public TankEntry registerEntry(RegistryTank r, double weight)
		{
			return new TankEntry(r, this.tank, this.name, weight);
		}
		
		public String getString()
		{
			return this.name + "," + this.weight + ",default";
		}
	}
	
	public TankEntry getRandomTank()
	{
		if (this.tankEntries.size() <= 0)
			throw new RuntimeException("the tank registry file is empty. please register some tanks!");
			
		double random = Math.random() * maxTankWeight;
		for (int i = 0; i < tankEntries.size(); i++)
		{
			TankEntry r = tankEntries.get(i);

			if (random >= r.startWeight && random < r.endWeight)
			{
				return r;
			}
		}
		
		return TankEntry.getUnknownEntry();
	}
	
	public TankEntry getEntry(String name)
	{		
		for (int i = 0; i < tankEntries.size(); i++)
		{
			TankEntry r = tankEntries.get(i);
			
			if (r.name.equals(name))
			{
				return r;
			}
		}
		
		return TankEntry.getUnknownEntry(name);
	}
	
	public TankEntry getEntry(int number)
	{		
		return tankEntries.get(number);
	}
}
