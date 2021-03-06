/*
 * Copyright (C) 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.alibaba.nacos.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.alibaba.nacos.NacosConfigProperties;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

import com.alibaba.nacos.api.config.ConfigService;

/**
 * @author xiaojing
 */
@Order(0)
public class NacosPropertySourceLocator implements PropertySourceLocator {

	private static final Logger logger = LoggerFactory
			.getLogger(NacosPropertySourceLocator.class);
	private static final String NACOS_PROPERTY_SOURCE_NAME = "NACOS";
	private static final String SEP1 = "-";
	private static final String DOT = ".";

	@Autowired
	private NacosConfigProperties nacosConfigProperties;

	private NacosPropertySourceBuilder nacosPropertySourceBuilder;

	@Override
	public PropertySource<?> locate(Environment env) {

		ConfigService configService = nacosConfigProperties.configServiceInstance();

		if (null == configService) {
			logger.warn(
					"no instance of config service found, can't load config from nacos");
			return null;
		}
		long timeout = nacosConfigProperties.getTimeout();
		nacosPropertySourceBuilder = new NacosPropertySourceBuilder(configService,
				timeout);

		String name = nacosConfigProperties.getName();

		String nacosGroup = nacosConfigProperties.getGroup();
		String dataIdPrefix = nacosConfigProperties.getPrefix();
		if (StringUtils.isEmpty(dataIdPrefix)) {
			dataIdPrefix = name;
		}

		String fileExtension = nacosConfigProperties.getFileExtension();

		CompositePropertySource composite = new CompositePropertySource(
				NACOS_PROPERTY_SOURCE_NAME);

		loadApplicationConfiguration(composite, nacosGroup, dataIdPrefix, fileExtension);

		return composite;
	}

	private void loadApplicationConfiguration(
			CompositePropertySource compositePropertySource, String nacosGroup,
			String dataIdPrefix, String fileExtension) {
		loadNacosDataIfPresent(compositePropertySource,
				dataIdPrefix + DOT + fileExtension, nacosGroup, fileExtension);
		for (String profile : nacosConfigProperties.getActiveProfiles()) {
			String dataId = dataIdPrefix + SEP1 + profile + DOT + fileExtension;
			loadNacosDataIfPresent(compositePropertySource, dataId, nacosGroup,
					fileExtension);
		}
	}

	private void loadNacosDataIfPresent(final CompositePropertySource composite,
			final String dataId, final String group, String fileExtension) {
		NacosPropertySource ps = nacosPropertySourceBuilder.build(dataId, group,
				fileExtension);
		if (ps != null) {
			composite.addFirstPropertySource(ps);
		}
	}
}
