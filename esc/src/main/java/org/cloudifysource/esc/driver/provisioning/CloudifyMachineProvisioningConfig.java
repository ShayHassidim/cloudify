/*******************************************************************************
 * Copyright (c) 2011 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.cloudifysource.esc.driver.provisioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudifysource.dsl.cloud.Cloud;
import org.cloudifysource.dsl.cloud.CloudTemplate;
import org.cloudifysource.dsl.internal.CloudifyConstants;
import org.openspaces.admin.pu.elastic.ElasticMachineProvisioningConfig;
import org.openspaces.core.util.StringProperties;
import org.openspaces.grid.gsm.capacity.CapacityRequirement;
import org.openspaces.grid.gsm.capacity.CapacityRequirements;
import org.openspaces.grid.gsm.capacity.CpuCapacityRequirement;
import org.openspaces.grid.gsm.capacity.DriveCapacityRequirement;
import org.openspaces.grid.gsm.capacity.MemoryCapacityRequirement;



public class CloudifyMachineProvisioningConfig implements ElasticMachineProvisioningConfig{

	private static final double NUMBER_OF_CPU_CORES_PER_MACHINE_DEFAULT = 4;
	private static final String NUMBER_OF_CPU_CORES_PER_MACHINE_KEY = "number-of-cpu-cores-per-machine";
	
	private static final String ZONES_KEY = "zones";
	private static final String ZONES_SEPARATOR = ",";
	private static final String[] ZONES_DEFAULT = new String[] {"agent"};
	
	private static final boolean DEDICATED_MANAGEMENT_MACHINES_DEFAULT = false;
	private static final String DEDICATED_MANAGEMENT_MACHINES_KEY = "dedicated-management-machines";
	
	private static final String RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_KEY =
			"reserved-memory-capacity-per-machine-megabytes";
	private static final long RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_DEFAULT = 256;
	
	private static final String RESERVED_CPU_PER_MACHINE_KEY = "reserved-cpu-cores-per-machine";
	private static final double RESERVED_CPU_PER_MACHINE_DEFAULT = 0.0;
	
	private static final String RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY =
			"resereved-drives-capacity-per-machine-megabytes";
	private static final Map<String, String> RESERVED_DRIVES_CAPACITY_PER_MACHINE_DEFAULT =
			new HashMap<String, String>();
	private static final String RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY_VALUE_SEPERATOR = "=";
	private static final String RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_PAIR_SEPERATOR = ",";
	
	private static final String LOCATOR_KEY = "locator";
	
	
	private StringProperties properties= new StringProperties(new HashMap<String, String>());

	
	public CloudifyMachineProvisioningConfig(final Cloud cloud, final CloudTemplate template, final String cloudFileContents, final String cloudTemplateName ) {
		
		setMinimumNumberOfCpuCoresPerMachine(template.getNumberOfCores());
		properties.putArray(ZONES_KEY, cloud.getProvider().getZones().toArray(new String[0]), ZONES_SEPARATOR);
		
		setDedicatedManagementMachines(cloud.getProvider().isDedicatedManagementMachines());
		
		setReservedMemoryCapacityPerMachineInMB(cloud.getProvider().getReservedMemoryCapacityPerMachineInMB());
		
		setCloudConfiguration(cloudFileContents);
		setCloudTemplateName(cloudTemplateName);
		
		
	}
	public CloudifyMachineProvisioningConfig(Map<String, String> properties) {
		this.properties = new StringProperties(properties);
	}
	@Override
	public String getBeanClassName() {
		return ElasticMachineProvisioningCloudifyAdapter.class.getName();
	}

	@Override
	public void setProperties(Map<String, String> properties) {
		this.properties = new StringProperties(properties);
	}

	@Override
	public Map<String, String> getProperties() {
		return this.properties.getProperties(); 
	}

	@Override
	public double getMinimumNumberOfCpuCoresPerMachine() {
		return properties.getDouble(NUMBER_OF_CPU_CORES_PER_MACHINE_KEY,
				NUMBER_OF_CPU_CORES_PER_MACHINE_DEFAULT);
	}
	
	public void setMinimumNumberOfCpuCoresPerMachine(final double minimumCpuCoresPerMachine) {
		properties.putDouble(NUMBER_OF_CPU_CORES_PER_MACHINE_KEY, minimumCpuCoresPerMachine);
	}

	public String  getCloudConfiguration() {
		return properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_CONFIGURATION);
	}
	
	public void setCloudConfiguration(final String cloudConfiguration) {
		properties.put(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_CONFIGURATION, cloudConfiguration);
	}
	
	public String  getCloudTemplateName() {
		return properties.get(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME);
	}
	
	public void setCloudTemplateName(final String cloudTemplateName) {
		properties.put(CloudifyConstants.ELASTIC_PROPERTIES_CLOUD_TEMPLATE_NAME, cloudTemplateName);
	}
	
	
	
	

	@Override
	public CapacityRequirements getReservedCapacityPerMachine() {
		final List<CapacityRequirement> requirements = new ArrayList<CapacityRequirement>();
		requirements.add(new MemoryCapacityRequirement(getReservedMemoryCapacityPerMachineInMB()));
		requirements.add(new CpuCapacityRequirement(getReservedCpuCapacityPerMachine()));
		final Map<String, Long> reservedDriveCapacity = getReservedDriveCapacityPerMachineInMB();
		for (final Entry<String, Long> entry : reservedDriveCapacity.entrySet()) {
			String drive = entry.getKey(); 
			requirements.add(new DriveCapacityRequirement(drive, entry.getValue()));
		}
		return new CapacityRequirements(requirements.toArray(new CapacityRequirement[requirements.size()]));

	}
	
	public Map<String, Long> getReservedDriveCapacityPerMachineInMB() {
		final Map<String, String> reserved =
				this.properties.getKeyValuePairs(
						RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY,
						RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_PAIR_SEPERATOR,
						RESREVED_DRIVES_CAPACITY_MEGABYTES_PER_MACHINE_KEY_VALUE_SEPERATOR,
						RESERVED_DRIVES_CAPACITY_PER_MACHINE_DEFAULT);

		final Map<String, Long> reservedInMB = new HashMap<String, Long>();
		for(final Map.Entry<String, String> entry : reserved.entrySet()){
            String drive = entry.getKey();
			reservedInMB.put(drive, Long.valueOf(entry.getValue()));
		}

		return reservedInMB;

	}

	public void setReservedCpuCapacityPerMachineInMB(final double reservedCpu) {
		this.properties.putDouble(RESERVED_CPU_PER_MACHINE_KEY, reservedCpu);
	}

	
	public double getReservedCpuCapacityPerMachine() {
		return this.properties.getDouble(RESERVED_CPU_PER_MACHINE_KEY, RESERVED_CPU_PER_MACHINE_DEFAULT);
	}

	public long getReservedMemoryCapacityPerMachineInMB() {
		return this.properties.getLong(RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_KEY,
				RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_DEFAULT);
	}
	
	public void setReservedMemoryCapacityPerMachineInMB(final long reservedInMB) {
		this.properties.putLong(RESERVED_MEMORY_CAPACITY_PER_MACHINE_MEGABYTES_KEY, reservedInMB);
	}
	
	@Override
	public String[] getGridServiceAgentZones() {
		return properties.getArray(ZONES_KEY, ZONES_SEPARATOR, ZONES_DEFAULT);
	}

	@Override
	public boolean isDedicatedManagementMachines() {
		return properties.getBoolean(DEDICATED_MANAGEMENT_MACHINES_KEY, DEDICATED_MANAGEMENT_MACHINES_DEFAULT);
	}
	
	public void setDedicatedManagementMachines(final boolean value) {
		properties.putBoolean(DEDICATED_MANAGEMENT_MACHINES_KEY, value);
	}

	@Override
	public boolean isGridServiceAgentZoneMandatory() {	
		return false;
	}
	
	public void setLocator(final String locator) {
		properties.put(LOCATOR_KEY, locator);
	}
	
	public String getLocator() {
		return properties.get(LOCATOR_KEY);
	}

}
