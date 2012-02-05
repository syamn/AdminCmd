/************************************************************************
 * This file is part of AdminCmd.									
 *																		
 * AdminCmd is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by	
 * the Free Software Foundation, either version 3 of the License, or		
 * (at your option) any later version.									
 *																		
 * AdminCmd is distributed in the hope that it will be useful,	
 * but WITHOUT ANY WARRANTY; without even the implied warranty of		
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the			
 * GNU General Public License for more details.							
 *																		
 * You should have received a copy of the GNU General Public License
 * along with AdminCmd.  If not, see <http://www.gnu.org/licenses/>.
 ************************************************************************/
package be.Balor.JUnit.Egg;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.Test;

import be.Balor.Manager.Commands.CommandArgs;
import be.Balor.Tools.Configuration.File.ExtendedConfiguration;
import be.Balor.Tools.Egg.DontHaveThePermissionException;
import be.Balor.Tools.Egg.EggType;
import be.Balor.Tools.Egg.EggTypeClassLoader;
import be.Balor.Tools.Egg.ProcessingArgsException;

/**
 * @author Balor (aka Antoine Aflalo)
 * 
 */
public class EggTypeTest {
	@Test
	public void registeringNewEggs() throws ProcessingArgsException, DontHaveThePermissionException {
		EggTypeClassLoader.addPackage(null, "be.Balor.JUnit.Egg");
		assertTrue(EggType.createEggType(null, new CommandArgs("-e Test -m Test")) instanceof TestEgg);
	}

	@Test
	public void serializationOfEggType() throws IOException, InvalidConfigurationException,
			ProcessingArgsException, DontHaveThePermissionException {
		ExtendedConfiguration.setClassLoader(this.getClass().getClassLoader());
		EggType<?> egg = EggType.createEggType(null, new CommandArgs("-e Test -t"));
		File test = new File("testEgg.yml");
		ExtendedConfiguration conf = ExtendedConfiguration.loadConfiguration(test);
		conf.set("egg", egg);
		conf.set("egg2", EggType.createEggType(null, new CommandArgs("-e Test")));
		conf.save();
		conf.reload();
		assertEquals(EggType.createEggType(null, new CommandArgs("-e Test -t")), conf.get("egg"));
		assertEquals(EggType.createEggType(null, new CommandArgs("-e Test")), conf.get("egg2"));
		test.deleteOnExit();

	}
}
