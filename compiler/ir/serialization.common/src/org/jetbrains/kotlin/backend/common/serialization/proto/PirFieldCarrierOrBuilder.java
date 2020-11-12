// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: compiler/ir/serialization.common/src/KotlinIr.proto

package org.jetbrains.kotlin.backend.common.serialization.proto;

public interface PirFieldCarrierOrBuilder extends
    // @@protoc_insertion_point(interface_extends:org.jetbrains.kotlin.backend.common.serialization.proto.PirFieldCarrier)
    org.jetbrains.kotlin.protobuf.MessageLiteOrBuilder {

  /**
   * <code>required int32 lastModified = 1;</code>
   */
  boolean hasLastModified();
  /**
   * <code>required int32 lastModified = 1;</code>
   */
  int getLastModified();

  /**
   * <code>optional int64 parentSymbol = 2;</code>
   */
  boolean hasParentSymbol();
  /**
   * <code>optional int64 parentSymbol = 2;</code>
   */
  long getParentSymbol();

  /**
   * <code>optional int32 origin = 3;</code>
   */
  boolean hasOrigin();
  /**
   * <code>optional int32 origin = 3;</code>
   */
  int getOrigin();

  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall annotation = 4;</code>
   */
  java.util.List<org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall> 
      getAnnotationList();
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall annotation = 4;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall getAnnotation(int index);
  /**
   * <code>repeated .org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall annotation = 4;</code>
   */
  int getAnnotationCount();

  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.IrType type = 5;</code>
   */
  boolean hasType();
  /**
   * <code>optional .org.jetbrains.kotlin.backend.common.serialization.proto.IrType type = 5;</code>
   */
  org.jetbrains.kotlin.backend.common.serialization.proto.IrType getType();

  /**
   * <code>optional int32 initializer = 6;</code>
   */
  boolean hasInitializer();
  /**
   * <code>optional int32 initializer = 6;</code>
   */
  int getInitializer();

  /**
   * <code>optional int64 correspondingPropertySymbol = 7;</code>
   */
  boolean hasCorrespondingPropertySymbol();
  /**
   * <code>optional int64 correspondingPropertySymbol = 7;</code>
   */
  long getCorrespondingPropertySymbol();
}