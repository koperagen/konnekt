import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.phases.CompilerContext
import arrow.meta.phases.ExtensionPhase
import arrow.meta.plugin.testing.CompilerTest
import arrow.meta.plugin.testing.Config
import arrow.meta.plugin.testing.assertThis
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.DescribeSpec
import konnekt.DataframePlugin
import konnekt.dataFramePackageProvider
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackageFragmentProviderImpl
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.Printer

object DataframeConfig : WithCompilerTestConfig {
  override val config: CompilerTest.Companion.() -> List<Config> = {
    listOf(addMetaPlugins(DataframePlugin()))
  }
}

class DataframePluginTest : WithCompilerTestConfig by DataframeConfig, DescribeSpec() {
  init {
    describe("Dataframe plugin") {
      it("should resolve extension properties in test()") {
        assertThis(CompilerTest(
            config = { listOf(addMetaPlugins(DataframePlugin())) },
            code = {
              """
              interface Column<T> {}
              interface DF<T> {}
              
              annotation class DataFrame
              
              @DataFrame
              interface DataInterface {
                val a: String
                val b: Int          
              }
              
              fun DF<DataInterface>.test() {
                a
                b
              }
          """.trimIndent().source
            },
            assert = { compiles }
        ))
      }

    }
  }
}

class StudyTests : AnnotationSpec() {

  fun test(meta: () -> Meta, code: () -> String) = assertThis(CompilerTest(
      config = { listOf(addMetaPlugins(meta())) },
      code = { code().source },
      assert = { compiles }
  ))

  fun buildMeta(builder: Meta.() -> List<ExtensionPhase>): Meta = object : Meta {
    override fun intercept(ctx: CompilerContext): List<CliPlugin> {
      return listOf("Random plugin name" { builder() })
    }
  }


  fun testPackageFragmentProvider(
      provider: CompilerContext.(project: Project, module: ModuleDescriptor, storageManager: StorageManager, trace: BindingTrace, moduleInfo: ModuleInfo?, lookupTracker: LookupTracker) -> PackageFragmentProvider?,
  ) = buildMeta {
    meta(
        enableIr(),
        packageFragmentProvider { project, module, storageManager, trace, moduleInfo, lookupTracker ->
          provider(project, module, storageManager, trace, moduleInfo, lookupTracker)
        }
    )
  }

  @Test
  fun `member scope contributes only requested names`() = test({
    testPackageFragmentProvider { project, module, storageManager, trace, moduleInfo, lookupTracker ->
      val names = mutableListOf<Name>()
      val a = object : PackageFragmentDescriptorImpl(module, FqName("test")) {

        override fun getMemberScope(): MemberScope {
          return object : MemberScopeImpl() {

            override fun getVariableNames(): Set<Name> {
              return setOf(Name.identifier("b"))
            }

            override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
              names.add(name)
              println(names)
              return emptyList()
            }

            override fun printScopeStructure(p: Printer) {
              TODO()
            }
          }
        }
      }
      PackageFragmentProviderImpl(listOf(a))
    }
  }, {
    """
      package test

      class A
      class B
      
      interface Column<T> {}
      annotation class DataFrame
      
      fun runC(f: Column<Int>.() -> Unit): String = TODO()
      
      @DataFrame
      interface DataInterface {
        val a: String
        val b: Int          
      }
      """.trimIndent()
  })


  @Test
  fun `descriptors from package provider doesn't seem to reach ir generation`() = test({
    buildMeta {
      meta(
          enableIr(),
          dataFramePackageProvider(),
          IrGeneration { compilerContext, moduleFragment, pluginContext ->
            Unit
          }
      )
    }
  }) {
    """
      interface Column<T> {}
      interface DF<T> {}
      
      annotation class DataFrame
      
      @DataFrame
      interface DataInterface {
        val a: String
        val b: Int          
      }
      
      fun DF<DataInterface>.test() {
        a
        b
      }
    """.trimIndent()
  }

  @Test
  fun `dump ir code for class cast`() = test({
    buildMeta {
      meta(
          enableIr(),
          irProperty {
            it
          },
          irDump()
      )
    }
  }, {
    """
      interface Test {
        operator fun get(s: String): Any
      }
      
      val Test.a: String get() {
       return this["a"] as String
     }
    """.trimIndent()
  })

  @Test
  fun `generate body for property`() = test({
    buildMeta {
      meta(
          enableIr(),
          irProperty {
            if (it.modality != Modality.FINAL) return@irProperty it
            it.getter!!.let { getter ->
              val scope = it.descriptor.extensionReceiverParameter!!.value.type.constructor.declarationDescriptor?.defaultType?.memberScope!!
              val symbol = referenceDeclaredFunction(scope.getContributedFunctions(Name.identifier("get"), NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS).single())
              val accessor = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type = irBuiltIns.anyType, symbol = symbol, 0, 1).apply {
                putValueArgument(0, IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.stringType, "${it.name}"))
                dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, getter.extensionReceiverParameter!!.symbol)
              }
              val expression = IrTypeOperatorCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type = getter.returnType, IrTypeOperator.CAST, getter.returnType, accessor)
              getter.body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                  IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, getter.returnType, getter.symbol, expression)
              ))
            }
            it
          },
          irDump()
      )
    }
  }, {
    """
      interface Column<T> {}
      interface DF<T> {
        operator fun get(s: String): Any
      }
      interface DataInterface {
        val a: String
        val b: Int          
      }
      
      val DF<DataInterface>.a: String get() = TODO()
    """.trimIndent()
  })

}