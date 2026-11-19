/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.openfeign.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import feign.MethodMetadata;

import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * {@link RequestHeader} parameter processor.
 *
 * @author Jakub Narloch
 * @author Abhijit Sarkar
 * @see AnnotatedParameterProcessor
 */
public class RequestHeaderParameterProcessor implements AnnotatedParameterProcessor {

	private static final Class<RequestHeader> ANNOTATION = RequestHeader.class;

	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return ANNOTATION;
	}

	@Override
	public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
		int parameterIndex = context.getParameterIndex();
		Class<?> parameterType = method.getParameterTypes()[parameterIndex];
		MethodMetadata data = context.getMethodMetadata();

		if (Map.class.isAssignableFrom(parameterType) || HttpHeaders.class.isAssignableFrom(parameterType)) {
			checkState(data.headerMapIndex() == null, "Header map can only be present once.");
			data.headerMapIndex(parameterIndex);

			return true;
		}

		String name = ANNOTATION.cast(annotation).value();
		checkState(emptyToNull(name) != null, "RequestHeader.value() was empty on parameter %s", parameterIndex);
		String templateName = uniqueTemplateName(name, data);
		context.setParameterName(templateName);

		Collection<String> header = context.setTemplateParameter(templateName, data.template().headers().get(name));
		data.template().header(name, header);
		return true;
	}

	/**
	 * Feign resolves template variables by name across the URL and the headers, so a
	 * header that shares its name with a path or query variable would also be expanded
	 * into the URL. Such a header gets a template variable name of its own.
	 */
	private static String uniqueTemplateName(String name, MethodMetadata data) {
		String templateName = name;
		int suffix = 0;
		while (isUsedElsewhere(templateName, data)) {
			templateName = name + "_header" + (suffix == 0 ? "" : suffix);
			suffix++;
		}
		return templateName;
	}

	private static boolean isUsedElsewhere(String templateName, MethodMetadata data) {
		if (data.template().url().contains("{" + templateName + "}")) {
			return true;
		}
		for (Collection<String> names : data.indexToName().values()) {
			if (names.contains(templateName)) {
				return true;
			}
		}
		return false;
	}

}
