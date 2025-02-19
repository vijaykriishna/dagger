/*
 * Copyright (C) 2024 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.internal.codegen.xprocessing;

import androidx.room.compiler.codegen.XClassName;
import androidx.room.compiler.codegen.XTypeName;
import androidx.room.compiler.processing.XType;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nullable;

/** Common names and convenience methods for XPoet {@link XTypeName} usage. */
public final class XTypeNames {

  // Dagger Core classnames
  public static final XClassName ASSISTED = XClassName.get("dagger.assisted", "Assisted");
  public static final XClassName ASSISTED_FACTORY =
      XClassName.get("dagger.assisted", "AssistedFactory");
  public static final XClassName ASSISTED_INJECT =
      XClassName.get("dagger.assisted", "AssistedInject");
  public static final XClassName BINDS = XClassName.get("dagger", "Binds");
  public static final XClassName BINDS_INSTANCE = XClassName.get("dagger", "BindsInstance");
  public static final XClassName BINDS_OPTIONAL_OF = XClassName.get("dagger", "BindsOptionalOf");
  public static final XClassName COMPONENT = XClassName.get("dagger", "Component");
  public static final XClassName COMPONENT_BUILDER =
      XClassName.get("dagger", "Component", "Builder");
  public static final XClassName COMPONENT_FACTORY =
      XClassName.get("dagger", "Component", "Factory");
  public static final XClassName DAGGER_PROCESSING_OPTIONS =
      XClassName.get("dagger", "DaggerProcessingOptions");
  public static final XClassName ELEMENTS_INTO_SET =
      XClassName.get("dagger.multibindings", "ElementsIntoSet");
  public static final XClassName INTO_MAP = XClassName.get("dagger.multibindings", "IntoMap");
  public static final XClassName INTO_SET = XClassName.get("dagger.multibindings", "IntoSet");
  public static final XClassName MAP_KEY = XClassName.get("dagger", "MapKey");
  public static final XClassName MODULE = XClassName.get("dagger", "Module");
  public static final XClassName MULTIBINDS = XClassName.get("dagger.multibindings", "Multibinds");
  public static final XClassName PROVIDES = XClassName.get("dagger", "Provides");
  public static final XClassName REUSABLE = XClassName.get("dagger", "Reusable");
  public static final XClassName SUBCOMPONENT = XClassName.get("dagger", "Subcomponent");
  public static final XClassName SUBCOMPONENT_BUILDER =
      XClassName.get("dagger", "Subcomponent", "Builder");
  public static final XClassName SUBCOMPONENT_FACTORY =
      XClassName.get("dagger", "Subcomponent", "Factory");

  // Dagger Internal classnames
  public static final XClassName IDENTIFIER_NAME_STRING =
      XClassName.get("dagger.internal", "IdentifierNameString");
  public static final XClassName KEEP_FIELD_TYPE =
      XClassName.get("dagger.internal", "KeepFieldType");
  public static final XClassName LAZY_CLASS_KEY =
      XClassName.get("dagger.multibindings", "LazyClassKey");
  public static final XClassName LAZY_CLASS_KEY_MAP =
      XClassName.get("dagger.internal", "LazyClassKeyMap");
  public static final XClassName LAZY_CLASS_KEY_MAP_FACTORY =
      XClassName.get("dagger.internal", "LazyClassKeyMap", "MapFactory");
  public static final XClassName LAZY_CLASS_KEY_MAP_PROVIDER_FACTORY =
      XClassName.get("dagger.internal", "LazyClassKeyMap", "MapProviderFactory");
  public static final XClassName LAZY_MAP_OF_PRODUCED_PRODUCER =
      XClassName.get("dagger.producers.internal", "LazyMapOfProducedProducer");
  public static final XClassName LAZY_MAP_OF_PRODUCER_PRODUCER =
      XClassName.get("dagger.producers.internal", "LazyMapOfProducerProducer");
  public static final XClassName LAZY_MAP_PRODUCER =
      XClassName.get("dagger.producers.internal", "LazyMapProducer");

  public static final XClassName DELEGATE_FACTORY =
      XClassName.get("dagger.internal", "DelegateFactory");
  public static final XClassName DOUBLE_CHECK = XClassName.get("dagger.internal", "DoubleCheck");

  public static final XClassName FACTORY = XClassName.get("dagger.internal", "Factory");
  public static final XClassName INJECTED_FIELD_SIGNATURE =
      XClassName.get("dagger.internal", "InjectedFieldSignature");
  public static final XClassName INSTANCE_FACTORY =
      XClassName.get("dagger.internal", "InstanceFactory");
  public static final XClassName MAP_BUILDER = XClassName.get("dagger.internal", "MapBuilder");
  public static final XClassName MAP_FACTORY = XClassName.get("dagger.internal", "MapFactory");
  public static final XClassName MAP_PROVIDER_FACTORY =
      XClassName.get("dagger.internal", "MapProviderFactory");
  public static final XClassName MEMBERS_INJECTOR = XClassName.get("dagger", "MembersInjector");
  public static final XClassName MEMBERS_INJECTORS =
      XClassName.get("dagger.internal", "MembersInjectors");
  public static final XClassName PROVIDER = XClassName.get("javax.inject", "Provider");
  public static final XClassName JAKARTA_PROVIDER = XClassName.get("jakarta.inject", "Provider");
  public static final XClassName DAGGER_PROVIDER = XClassName.get("dagger.internal", "Provider");
  public static final XClassName DAGGER_PROVIDERS = XClassName.get("dagger.internal", "Providers");
  public static final XClassName PROVIDER_OF_LAZY =
      XClassName.get("dagger.internal", "ProviderOfLazy");
  public static final XClassName SCOPE_METADATA =
      XClassName.get("dagger.internal", "ScopeMetadata");
  public static final XClassName QUALIFIER_METADATA =
      XClassName.get("dagger.internal", "QualifierMetadata");
  public static final XClassName SET_FACTORY = XClassName.get("dagger.internal", "SetFactory");
  public static final XClassName SINGLE_CHECK = XClassName.get("dagger.internal", "SingleCheck");
  public static final XClassName LAZY = XClassName.get("dagger", "Lazy");

  // Dagger Producers classnames
  public static final XClassName ABSTRACT_PRODUCER =
      XClassName.get("dagger.producers.internal", "AbstractProducer");
  public static final XClassName ABSTRACT_PRODUCES_METHOD_PRODUCER =
      XClassName.get("dagger.producers.internal", "AbstractProducesMethodProducer");
  public static final XClassName CANCELLATION_LISTENER =
      XClassName.get("dagger.producers.internal", "CancellationListener");
  public static final XClassName CANCELLATION_POLICY =
      XClassName.get("dagger.producers", "CancellationPolicy");
  public static final XClassName DELEGATE_PRODUCER =
      XClassName.get("dagger.producers.internal", "DelegateProducer");
  public static final XClassName DEPENDENCY_METHOD_PRODUCER =
      XClassName.get("dagger.producers.internal", "DependencyMethodProducer");
  public static final XClassName MAP_OF_PRODUCED_PRODUCER =
      XClassName.get("dagger.producers.internal", "MapOfProducedProducer");
  public static final XClassName MAP_OF_PRODUCER_PRODUCER =
      XClassName.get("dagger.producers.internal", "MapOfProducerProducer");
  public static final XClassName MAP_PRODUCER =
      XClassName.get("dagger.producers.internal", "MapProducer");
  public static final XClassName MONITORS =
      XClassName.get("dagger.producers.monitoring.internal", "Monitors");
  public static final XClassName PRODUCED = XClassName.get("dagger.producers", "Produced");
  public static final XClassName PRODUCER = XClassName.get("dagger.producers", "Producer");
  public static final XClassName PRODUCERS =
      XClassName.get("dagger.producers.internal", "Producers");
  public static final XClassName PRODUCER_MODULE =
      XClassName.get("dagger.producers", "ProducerModule");
  public static final XClassName PRODUCES = XClassName.get("dagger.producers", "Produces");
  public static final XClassName PRODUCTION = XClassName.get("dagger.producers", "Production");
  public static final XClassName PRODUCTION_COMPONENT =
      XClassName.get("dagger.producers", "ProductionComponent");
  public static final XClassName PRODUCTION_COMPONENT_BUILDER =
      XClassName.get("dagger.producers", "ProductionComponent", "Builder");
  public static final XClassName PRODUCTION_COMPONENT_FACTORY =
      XClassName.get("dagger.producers", "ProductionComponent", "Factory");
  public static final XClassName PRODUCTION_EXECTUTOR_MODULE =
      XClassName.get("dagger.producers.internal", "ProductionExecutorModule");
  public static final XClassName PRODUCTION_IMPLEMENTATION =
      XClassName.get("dagger.producers.internal", "ProductionImplementation");
  public static final XClassName PRODUCTION_SUBCOMPONENT =
      XClassName.get("dagger.producers", "ProductionSubcomponent");
  public static final XClassName PRODUCTION_SUBCOMPONENT_BUILDER =
      XClassName.get("dagger.producers", "ProductionSubcomponent", "Builder");
  public static final XClassName PRODUCTION_SUBCOMPONENT_FACTORY =
      XClassName.get("dagger.producers", "ProductionSubcomponent", "Factory");
  public static final XClassName PRODUCER_TOKEN =
      XClassName.get("dagger.producers.monitoring", "ProducerToken");
  public static final XClassName PRODUCTION_COMPONENT_MONITOR =
      XClassName.get("dagger.producers.monitoring", "ProductionComponentMonitor");
  public static final XClassName PRODUCTION_COMPONENT_MONITOR_FACTORY =
      XClassName.get("dagger.producers.monitoring", "ProductionComponentMonitor", "Factory");
  public static final XClassName SET_OF_PRODUCED_PRODUCER =
      XClassName.get("dagger.producers.internal", "SetOfProducedProducer");
  public static final XClassName SET_PRODUCER =
      XClassName.get("dagger.producers.internal", "SetProducer");
  public static final XClassName PRODUCTION_SCOPE =
      XClassName.get("dagger.producers", "ProductionScope");

  // Other classnames
  public static final XClassName EXECUTOR = XClassName.get("java.util.concurrent", "Executor");
  public static final XClassName ERROR = XClassName.get("java.lang", "Error");
  public static final XClassName EXCEPTION = XClassName.get("java.lang", "Exception");
  public static final XClassName RUNTIME_EXCEPTION =
      XClassName.get("java.lang", "RuntimeException");

  public static final XClassName KOTLIN_METADATA = XClassName.get("kotlin", "Metadata");
  public static final XClassName IMMUTABLE_MAP =
      XClassName.get("com.google.common.collect", "ImmutableMap");
  public static final XClassName SINGLETON = XClassName.get("jakarta.inject", "Singleton");
  public static final XClassName SINGLETON_JAVAX = XClassName.get("javax.inject", "Singleton");
  public static final XClassName SCOPE = XClassName.get("jakarta.inject", "Scope");
  public static final XClassName SCOPE_JAVAX = XClassName.get("javax.inject", "Scope");
  public static final XClassName INJECT = XClassName.get("jakarta.inject", "Inject");
  public static final XClassName INJECT_JAVAX = XClassName.get("javax.inject", "Inject");
  public static final XClassName QUALIFIER = XClassName.get("jakarta.inject", "Qualifier");
  public static final XClassName QUALIFIER_JAVAX = XClassName.get("javax.inject", "Qualifier");
  public static final XClassName IMMUTABLE_SET =
      XClassName.get("com.google.common.collect", "ImmutableSet");
  public static final XClassName FUTURES =
      XClassName.get("com.google.common.util.concurrent", "Futures");
  public static final XClassName LISTENABLE_FUTURE =
      XClassName.get("com.google.common.util.concurrent", "ListenableFuture");
  public static final XClassName FLUENT_FUTURE =
      XClassName.get("com.google.common.util.concurrent", "FluentFuture");
  public static final XClassName GUAVA_OPTIONAL =
      XClassName.get("com.google.common.base", "Optional");
  public static final XClassName GUAVA_FUNCTION =
      XClassName.get("com.google.common.base", "Function");
  public static final XClassName JDK_OPTIONAL = XClassName.get("java.util", "Optional");
  public static final XClassName OVERRIDE = XClassName.get("java.lang", "Override");
  public static final XClassName JVM_STATIC = XClassName.get("kotlin.jvm", "JvmStatic");
  public static final XClassName CLASS = XClassName.get("java.lang", "Class");
  public static final XClassName KCLASS = XClassName.get("kotlin.reflect", "KClass");

  public static XTypeName abstractProducerOf(XTypeName typeName) {
    return ABSTRACT_PRODUCER.parametrizedBy(typeName);
  }

  public static XTypeName factoryOf(XTypeName factoryType) {
    return FACTORY.parametrizedBy(factoryType);
  }

  public static XTypeName lazyOf(XTypeName typeName) {
    return LAZY.parametrizedBy(typeName);
  }

  public static XTypeName listOf(XTypeName typeName) {
    return XTypeName.LIST.parametrizedBy(typeName);
  }

  public static XTypeName listenableFutureOf(XTypeName typeName) {
    return LISTENABLE_FUTURE.parametrizedBy(typeName);
  }

  public static XTypeName membersInjectorOf(XTypeName membersInjectorType) {
    return MEMBERS_INJECTOR.parametrizedBy(membersInjectorType);
  }

  public static XTypeName producedOf(XTypeName typeName) {
    return PRODUCED.parametrizedBy(typeName);
  }

  public static XTypeName producerOf(XTypeName typeName) {
    return PRODUCER.parametrizedBy(typeName);
  }

  public static XTypeName dependencyMethodProducerOf(XTypeName typeName) {
    return DEPENDENCY_METHOD_PRODUCER.parametrizedBy(typeName);
  }

  public static XTypeName providerOf(XTypeName typeName) {
    return PROVIDER.parametrizedBy(typeName);
  }

  public static XTypeName daggerProviderOf(XTypeName typeName) {
    return DAGGER_PROVIDER.parametrizedBy(typeName);
  }

  public static XTypeName setOf(XTypeName elementType) {
    return XTypeName.SET.parametrizedBy(elementType);
  }

  private static final ImmutableSet<XClassName> FUTURE_TYPES =
      ImmutableSet.of(LISTENABLE_FUTURE, FLUENT_FUTURE);

  public static boolean isFutureType(XType type) {
    return isFutureType(type.asTypeName());
  }

  public static boolean isFutureType(XTypeName typeName) {
    return FUTURE_TYPES.contains(typeName.getRawTypeName());
  }

  @Nullable
  public static XClassName enclosingClassName(XClassName className) {
    int size = className.getSimpleNames().size();
    return size == 1
        ? null
        : XClassName.get(
            className.getPackageName(),
            className.getSimpleNames().subList(0, size - 1).toArray(new String[0]));
  }

  private XTypeNames() {}
}
