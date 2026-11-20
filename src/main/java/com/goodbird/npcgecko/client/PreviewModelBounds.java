package com.goodbird.npcgecko.client;

import software.bernie.geckolib3.core.snapshot.BoneSnapshot;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;
import software.bernie.geckolib3.util.MatrixStack;

import javax.vecmath.Vector4f;

/** Neutral-pose bounds, using the same transforms as Gecko's renderer without changing its bones. */
final class PreviewModelBounds {
    private final double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
    private final double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};

    static PreviewModelBounds measure(GeoModel model) {
        PreviewModelBounds bounds = new PreviewModelBounds();
        MatrixStack matrices = new MatrixStack();
        for (GeoBone bone : model.topLevelBones) bounds.visit(bone, matrices);
        return bounds;
    }

    boolean valid() {
        return Double.isFinite(width()) && Double.isFinite(height()) && width() > 0 && height() > 0;
    }

    double width() { return Math.hypot(max[0] - min[0], max[2] - min[2]); }
    double width(float yaw) {
        double angle = Math.toRadians(yaw);
        return Math.abs(Math.cos(angle)) * (max[0] - min[0])
            + Math.abs(Math.sin(angle)) * (max[2] - min[2]);
    }
    double height() { return max[1] - min[1]; }
    double center(int axis) { return (min[axis] + max[axis]) / 2; }

    private void visit(GeoBone bone, MatrixStack matrices) {
        matrices.push();
        BoneSnapshot pose = bone.getInitialSnapshot();
        if (pose == null) matrices.translate(bone);
        else matrices.translate(-pose.positionOffsetX / 16, pose.positionOffsetY / 16, pose.positionOffsetZ / 16);
        matrices.moveToPivot(bone);
        if (pose == null) {
            matrices.rotate(bone);
            matrices.scale(bone);
        } else {
            matrices.rotateZ(pose.rotationValueZ);
            matrices.rotateY(pose.rotationValueY);
            matrices.rotateX(pose.rotationValueX);
            matrices.scale(pose.scaleValueX, pose.scaleValueY, pose.scaleValueZ);
        }
        matrices.moveBackFromPivot(bone);
        if (!Boolean.TRUE.equals(bone.dontRender)) for (GeoCube cube : bone.childCubes) {
            matrices.push();
            matrices.moveToPivot(cube);
            matrices.rotate(cube);
            matrices.moveBackFromPivot(cube);
            for (GeoQuad quad : cube.quads) {
                if (quad == null) continue;
                for (GeoVertex vertex : quad.vertices) {
                    Vector4f point = new Vector4f(vertex.position.x, vertex.position.y, vertex.position.z, 1);
                    matrices.getModelMatrix().transform(point);
                    double[] coordinates = {point.x, point.y, point.z};
                    for (int axis = 0; axis < 3; axis++) {
                        min[axis] = Math.min(min[axis], coordinates[axis]);
                        max[axis] = Math.max(max[axis], coordinates[axis]);
                    }
                }
            }
            matrices.pop();
        }
        for (GeoBone child : bone.childBones) visit(child, matrices);
        matrices.pop();
    }
}
