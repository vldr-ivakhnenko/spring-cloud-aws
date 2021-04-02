/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.awspring.cloud.v3.autoconfigure.core;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.awspring.cloud.v3.core.region.StaticRegionProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.providers.AwsProfileRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.regions.providers.InstanceProfileRegionProvider;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration} for {@link AwsRegionProvider}.
 *
 * @author Siva Katamreddy
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RegionProperties.class)
public class RegionProviderAutoConfiguration {

	private final RegionProperties properties;

	public RegionProviderAutoConfiguration(RegionProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public AwsRegionProvider regionProvider() {
		final List<AwsRegionProvider> providers = new ArrayList<>();

		if (this.properties.isStatic()) {
			providers.add(new StaticRegionProvider(this.properties.getStatic()));
		}

		if (this.properties.isInstanceProfile()) {
			providers.add(new InstanceProfileRegionProvider());
		}

		Profile profile = this.properties.getProfile();
		if (profile != null && profile.getName() != null) {
			providers.add(createProfileRegionProvider());
		}

		if (providers.isEmpty()) {
			return DefaultAwsRegionProviderChain.builder().build();
		}
		else {
			return new AwsRegionProviderChain(providers.toArray(new AwsRegionProvider[0]));
		}
	}

	private AwsProfileRegionProvider createProfileRegionProvider() {
		Profile profile = this.properties.getProfile();
		Supplier<ProfileFile> profileFileFn = () -> {
			ProfileFile profileFile = ProfileFile.builder().type(ProfileFile.Type.CONFIGURATION)
					.content(Paths.get(profile.getPath())).build();
			ProfileFile defaultProfileFile = ProfileFile.defaultProfileFile();
			return profile.getPath() != null ? profileFile : defaultProfileFile;
		};
		return new AwsProfileRegionProvider(profileFileFn, profile.getName());
	}

}
