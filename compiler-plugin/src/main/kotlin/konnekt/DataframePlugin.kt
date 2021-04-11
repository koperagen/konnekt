package konnekt

import arrow.meta.CliPlugin
import arrow.meta.Meta
import arrow.meta.invoke
import arrow.meta.phases.CompilerContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.Printer

class DataframePlugin : Meta {
  override fun intercept(ctx: CompilerContext): List<CliPlugin> =
      listOf(dataframePlugin)
}

val Meta.dataframePlugin: CliPlugin
  get() = "Dataframe Plugin" {
    meta(
        enableIr(),
        dataFramePackageProvider(),
        irProperty {
          if (it.descriptor.kind != CallableMemberDescriptor.Kind.SYNTHESIZED) return@irProperty it
          it.getter?.let { getter ->
            val symbol = referenceDeclaredFunction(it.descriptor.extensionReceiverParameter!!.value.type.memberScope.getContributedFunctions(Name.identifier("get"), NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)!!.single())
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

fun Meta.dataFramePackageProvider() =
    packageFragmentProvider { project, module, storageManager, trace, moduleInfo, lookupTracker ->
      object : PackageFragmentProvider {
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
          return module.getPackage(fqName).let { packageViewDescriptor ->
            listOf(object : PackageFragmentDescriptorImpl(module, fqName) {
              private val a = this
              private val extensions by lazy {
                val columnType = module.resolveClassByFqName(COLUMN_FQNAME, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)?.defaultType
                    ?: error("")
                val dataframeType = module.resolveClassByFqName(DATAFRAME_FQNAME, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)?.defaultType
                    ?: error("")

                val dataframeInterfaces = packageViewDescriptor.memberScope
                    .getContributedDescriptors { true }
                    .filterIsInstance<ClassDescriptor>()
                    .filter { it.annotations.hasAnnotation(DATAFRAME_ANNOTATION_FQNAME) }

                dataframeInterfaces.flatMap {
                  val projectedDataframeType = dataframeType.replace(listOf(it.defaultType.asTypeProjection()))
                  val properties = it.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.VARIABLES)
                      .asSequence()
                      .filterIsInstance<PropertyDescriptor>()

                  properties.map { /*createColumnAccessor(a, projectedDataframeType, columnType, it) + */createValueAccessor(a, projectedDataframeType, it) }
                }
              }

              override fun getMemberScope(): MemberScope {
                return object : MemberScopeImpl() {
                  override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
                    return extensions.filter { it.name == name }
                  }

                  override fun printScopeStructure(p: Printer) {
                    TODO()
                  }
                }
              }
            })
          }
        }

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
          return emptyList()
        }
      }
    }

fun createColumnAccessor(containingDeclarationDescriptor: DeclarationDescriptor, dataframeType: KotlinType, columnType: KotlinType, it: PropertyDescriptor): PropertyDescriptor {
  val returnType = columnType.replace(listOf(it.returnType!!.asTypeProjection()))
  val descriptor = PropertyDescriptorImpl.create(
      containingDeclarationDescriptor,
      Annotations.EMPTY,
      Modality.FINAL,
      Visibilities.PUBLIC,
      false,
      it.name,
      CallableMemberDescriptor.Kind.SYNTHESIZED,
      containingDeclarationDescriptor.toSourceElement,
      false,
      false,
      false,
      false,
      false,
      false
  )

  val receiverDescriptor = DescriptorFactory.createExtensionReceiverParameterForCallable(descriptor, dataframeType, Annotations.EMPTY)
  descriptor.setType(returnType, emptyList(), null, receiverDescriptor)
  val getter = DescriptorFactory.createDefaultGetter(descriptor, Annotations.EMPTY).also { it.initialize(null) }
  descriptor.initialize(getter, null)

  return descriptor
}

fun createValueAccessor(containingDeclarationDescriptor: DeclarationDescriptor, dataframeType: KotlinType, it: PropertyDescriptor): PropertyDescriptor {
  val returnType = it.returnType!!

  val descriptor = PropertyDescriptorImpl.create(
      containingDeclarationDescriptor,
      Annotations.EMPTY,
      Modality.FINAL,
      Visibilities.PUBLIC,
      false,
      it.name,
      CallableMemberDescriptor.Kind.SYNTHESIZED,
      containingDeclarationDescriptor.toSourceElement,
      false,
      false,
      false,
      false,
      false,
      false
  )

  val receiverDescriptor = DescriptorFactory.createExtensionReceiverParameterForCallable(descriptor, dataframeType, Annotations.EMPTY)
  descriptor.setType(returnType, emptyList(), null, receiverDescriptor)
  val getter = DescriptorFactory.createDefaultGetter(descriptor, Annotations.EMPTY).also { it.initialize(null) }
  descriptor.initialize(getter, null)

  return descriptor
}

fun CompilerContext.log(a: Any) = ctx.messageCollector?.report(CompilerMessageSeverity.INFO, a.toString(), null)

val COLUMN_FQNAME = FqName.fromSegments(listOf("Column"))
val DATAFRAME_ANNOTATION_FQNAME = FqName.fromSegments(listOf("DataFrame"))
val DATAFRAME_FQNAME = FqName.fromSegments(listOf("DF"))