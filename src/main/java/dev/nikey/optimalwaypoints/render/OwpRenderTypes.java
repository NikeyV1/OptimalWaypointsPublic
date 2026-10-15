package dev.nikey.optimalwaypoints.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.nikey.optimalwaypoints.OptimalWaypoints;
import dev.nikey.optimalwaypoints.mixin.RenderTypeInvoker;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class OwpRenderTypes {
	private static RenderType linesSeeThrough;
	private static RenderType linesDepth;
	private static RenderType fillSeeThrough;
	private static RenderType fillDepth;

	private OwpRenderTypes() {
	}

	public static RenderType lines(boolean seeThrough) {
		if (seeThrough) {
			if (linesSeeThrough == null) {
				linesSeeThrough = create("lines_see_through", linesPipeline("lines_see_through", CompareOp.ALWAYS_PASS));
			}
			return linesSeeThrough;
		}
		if (linesDepth == null) {
			linesDepth = create("lines_depth", linesPipeline("lines_depth", CompareOp.GREATER_THAN_OR_EQUAL));
		}
		return linesDepth;
	}

	public static RenderType fill(boolean seeThrough) {
		if (seeThrough) {
			if (fillSeeThrough == null) {
				fillSeeThrough = create("fill_see_through", fillPipeline("fill_see_through", CompareOp.ALWAYS_PASS));
			}
			return fillSeeThrough;
		}
		if (fillDepth == null) {
			fillDepth = create("fill_depth", fillPipeline("fill_depth", CompareOp.GREATER_THAN_OR_EQUAL));
		}
		return fillDepth;
	}

	private static RenderPipeline linesPipeline(String name, CompareOp depthTest) {
		return RenderPipeline.builder()
			.withLocation(OptimalWaypoints.id("pipeline/" + name))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
			.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withBindGroupLayout(BindGroupLayouts.FOG)
			.withVertexShader("core/rendertype_lines")
			.withFragmentShader("core/rendertype_lines")
			.withCull(false)
			.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
			.withPrimitiveTopology(PrimitiveTopology.LINES)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withDepthStencilState(new DepthStencilState(depthTest, false))
			.build();
	}

	private static RenderPipeline fillPipeline(String name, CompareOp depthTest) {
		return RenderPipeline.builder()
			.withLocation(OptimalWaypoints.id("pipeline/" + name))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
			.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withVertexShader("core/position_color")
			.withFragmentShader("core/position_color")
			.withCull(false)
			.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withDepthStencilState(new DepthStencilState(depthTest, false))
			.build();
	}

	private static RenderType create(String name, RenderPipeline pipeline) {
		RenderSetup setup = RenderSetup.builder(pipeline).setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING).createRenderSetup();
		return RenderTypeInvoker.optimalwaypoints$create(OptimalWaypoints.MOD_ID + "_" + name, setup);
	}
}
