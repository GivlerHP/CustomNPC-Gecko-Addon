package com.goodbird.npcgecko.client;

import com.goodbird.npcgecko.data.CustomModelData;
import com.goodbird.npcgecko.entity.EntityCustomModel;
import com.goodbird.npcgecko.mixin.IDataDisplay;
import kamkeel.npcs.addon.GeckoAddon;
import kamkeel.npcs.addon.client.GeckoAddonClient;
import kamkeel.npcs.addon.client.GeckoAddonClient.GestureStart;
import kamkeel.npcs.addon.client.GeckoAddonClient.GestureAvailability;
import kamkeel.npcs.addon.client.GeckoAddonClient.ModelAsset;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import noppes.npcs.controllers.data.DialogAnimation;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.core.keyframe.VectorKeyFrameList;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.geo.raw.pojo.ModelProperties;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class GeckoDialogueAnimations implements GeckoAddonClient.ModelResources {
    private Object indexedRevision;
    private List<String> files = Collections.emptyList();
    private List<String> models = Collections.emptyList();
    private final Map<String, List<String>> clips = new HashMap<>();
    private final Map<String, ModelAsset> assets = new HashMap<>();
    private final Map<GeoModel, java.util.Set<String>> boneNames = new java.util.IdentityHashMap<>();
    private final Map<String, List<String>> compatibility = new HashMap<>();
    private final Map<GeoModel, PreviewModelBounds> previewBounds = new java.util.IdentityHashMap<>();
    private GeoModel checkedGeometry;
    private Animation checkedClip;
    private List<String> missingBones = Collections.emptyList();

    static void register() {
        GeckoAddonClient.Instance.registerDialogueAnimations(new GeckoDialogueAnimations());
    }

    @Override
    public Object resourceRevision() {
        return GeckoLibCache.getInstance().getResourceRevision();
    }

    private void refreshIndex() {
        Object revision = resourceRevision();
        if (indexedRevision == revision) return;
        indexedRevision = revision;
        clips.clear();
        assets.clear();
        boneNames.clear();
        compatibility.clear();
        previewBounds.clear();
        checkedGeometry = null;
        checkedClip = null;
        missingBones = Collections.emptyList();
        List<String> paths = new ArrayList<>();
        GeckoLibCache.getInstance().getAnimations().keySet().forEach(location -> {
            String path = location.toString();
            if (DialogAnimation.validFile(path)) paths.add(path);
        });
        files = sortedSnapshot(paths);
        List<String> geometries = new ArrayList<>();
        GeckoLibCache.getInstance().getGeoModels().keySet().forEach(location -> {
            String path = location.toString();
            if (!path.equals("npcgecko:geo/modelnotfound.geo.json") && !path.equals("npcgecko:geo/animfilenotfound.geo.json")) {
                geometries.add(path);
            }
        });
        geometries.sort(Comparator.comparing(GeckoAddonClient::resourceName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Comparator.naturalOrder()));
        models = Collections.unmodifiableList(geometries);
    }

    private static List<String> sortedSnapshot(List<String> values) {
        Collections.sort(values);
        return Collections.unmodifiableList(values);
    }

    @Override
    public List<String> files() {
        refreshIndex();
        return files;
    }

    @Override
    public List<String> models() {
        refreshIndex();
        return models;
    }

    @Override
    public String geometry(EntityNPCInterface npc) {
        return hasModel(npc) ? new ResourceLocation(modelData(npc).getModel()).toString() : "";
    }

    @Override
    public ModelAsset model(String geometry) {
        refreshIndex();
        if (geometry.isEmpty()) return null;
        return assets.computeIfAbsent(geometry, this::describeModel);
    }

    private ModelAsset describeModel(String id) {
        ResourceLocation location = new ResourceLocation(id);
        GeckoLibCache cache = GeckoLibCache.getInstance();
        GeoModel geometry = cache.getGeoModels().get(location);
        if (geometry == null) return null;
        String origin = "gecko.origin." + cache.getResourceOrigin(location, true).name().toLowerCase(Locale.ROOT);
        String head = geometry.getBone("head").isPresent() ? "head" : geometry.getBone("Head").isPresent() ? "Head" : "";
        if (id.equals(CustomModelData.DEFAULT_MODEL)) {
            return new ModelAsset(id, CustomModelData.DEFAULT_TEXTURE, CustomModelData.DEFAULT_ANIMATION, head, origin);
        }
        String relative = location.getResourcePath();
        if (relative.startsWith("geo/")) relative = relative.substring(4);
        String stem = relative.endsWith(".geo.json") ? relative.substring(0, relative.length() - 9)
            : relative.endsWith(".json") ? relative.substring(0, relative.length() - 5) : relative;
        String name = stem.substring(stem.lastIndexOf('/') + 1);
        String texture = matchingTexture(location.getResourceDomain(), stem, name);
        String file = matchingAnimation(location.getResourceDomain(), stem, name, geometry);
        return new ModelAsset(id, texture, file, head, origin);
    }

    private static String matchingTexture(String domain, String stem, String name) {
        for (String path : new String[]{"textures/npc/" + stem, "textures/model/entity/" + stem, "textures/entity/" + stem,
            "textures/" + stem, "textures/npc/" + name, "textures/model/" + name, "textures/entity/" + name}) {
            ResourceLocation texture = new ResourceLocation(domain, path + ".png");
            try (InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(texture).getInputStream()) {
                if (stream != null) return texture.toString();
            } catch (IOException ignored) {
                // Resource packs can use different directory layouts.
            }
        }
        return "";
    }

    private static String matchingAnimation(String domain, String stem, String name, GeoModel geometry) {
        Map<ResourceLocation, AnimationFile> animations = GeckoLibCache.getInstance().getAnimations();
        for (String path : new String[]{stem, name}) {
            for (String suffix : new String[]{".animation.json", ".animations.json", ".json"}) {
                String prefix = "custom".equals(domain) ? "" : "animations/";
                ResourceLocation location = new ResourceLocation(domain, prefix + path + suffix);
                AnimationFile file = animations.get(location);
                if (file != null && file.getAllAnimations().stream().anyMatch(clip ->
                    clip.boneAnimations.stream().anyMatch(track -> geometry.getBone(track.boneName).isPresent()))) {
                    return location.toString();
                }
            }
        }
        return "";
    }

    @Override
    public void applyModel(EntityNPCInterface draft, ModelAsset asset, boolean useMatchingResources) {
        CustomModelData data = modelData(draft);
        boolean changed = !asset.geometry.equals(geometry(draft));
        data.setModel(asset.geometry);
        if (changed && !asset.headBone.isEmpty()) {
            GeoModel geometry = GeckoLibCache.getInstance().getGeoModels().get(new ResourceLocation(asset.geometry));
            if (!geometry.getBone(data.getHeadBoneName()).isPresent()) data.setHeadBoneName(asset.headBone);
        }
        if (!useMatchingResources) return;
        if (!asset.texture.isEmpty()) {
            draft.display.skinType = 0;
            draft.display.texture = asset.texture;
            draft.textureLocation = null;
        }
        if (!asset.animationFile.isEmpty()) data.setAnimFile(asset.animationFile);
    }

    @Override
    public List<String> clips(String file) {
        refreshIndex();
        if (!DialogAnimation.validFile(file)) return Collections.emptyList();
        return clips.computeIfAbsent(file, path -> {
            AnimationFile resource = GeckoLibCache.getInstance().getAnimations().get(new ResourceLocation(path));
            if (resource == null) return Collections.emptyList();
            List<String> names = new ArrayList<>();
            for (Animation clip : resource.getAllAnimations()) {
                if (DialogAnimation.of(path, clip.animationName).isPresent()) names.add(clip.animationName);
            }
            return sortedSnapshot(names);
        });
    }

    @Override
    public String animationFile(EntityNPCInterface npc) {
        return hasModel(npc) ? configuredFile((IDataDisplay) npc.display) : "";
    }

    private static boolean hasModel(EntityNPCInterface npc) {
        return npc instanceof EntityCustomNpc && npc.display instanceof IDataDisplay
            && ((EntityCustomNpc) npc).modelData.getEntityClass() == EntityCustomModel.class;
    }

    private static CustomModelData modelData(EntityNPCInterface npc) {
        return ((IDataDisplay) npc.display).getCustomModelData();
    }

    private static String configuredFile(IDataDisplay display) {
        String file = display.getCustomModelData().getAnimFile();
        return file.isEmpty() ? "" : new ResourceLocation(file).toString();
    }

    @Override
    public GestureAvailability gestureAvailability(EntityNPCInterface npc, DialogAnimation reference) {
        if (!hasModel(npc)) return GestureAvailability.NO_MODEL;
        IDataDisplay display = (IDataDisplay) npc.display;
        if (!reference.isPresent() || !reference.file.equals(configuredFile(display))) return GestureAvailability.DIFFERENT_FILE;
        return clipAvailability(geometry(npc), reference);
    }

    @Override
    public GestureAvailability clipAvailability(String geometryId, DialogAnimation reference) {
        if (geometryId.isEmpty()) return GestureAvailability.NO_MODEL;
        GeckoLibCache cache = GeckoLibCache.getInstance();
        GeoModel geometry = cache.getGeoModels().get(new ResourceLocation(geometryId));
        if (geometry == null) return GestureAvailability.MISSING_GEOMETRY;
        AnimationFile file = cache.getAnimations().get(new ResourceLocation(reference.file));
        if (file == null) return GestureAvailability.MISSING_FILE;
        Animation clip = file.getAnimation(reference.clip);
        if (clip == null) return GestureAvailability.MISSING_CLIP;
        refreshIndex();
        java.util.Set<String> names = boneNames.computeIfAbsent(geometry, model -> {
            java.util.Set<String> result = new java.util.HashSet<>();
            collectBones(model.topLevelBones, result);
            return result;
        });
        String key = geometryId + "|" + reference.file + "|" + reference.clip;
        missingBones = compatibility.computeIfAbsent(key, unused -> {
            List<String> missing = new ArrayList<>();
            clip.boneAnimations.forEach(track -> { if (!names.contains(track.boneName)) missing.add(track.boneName); });
            return Collections.unmodifiableList(missing);
        });
        if (missingBones.isEmpty()) return GestureAvailability.READY;
        return missingBones.size() == clip.boneAnimations.size()
            ? GestureAvailability.NO_MATCHING_BONES : GestureAvailability.PARTIAL_BONES;
    }

    private static void collectBones(List<software.bernie.geckolib3.geo.render.built.GeoBone> bones, java.util.Set<String> names) {
        for (software.bernie.geckolib3.geo.render.built.GeoBone bone : bones) {
            names.add(bone.name);
            collectBones(bone.childBones, names);
        }
    }

    @Override
    public List<String> missingGestureBones(EntityNPCInterface npc, DialogAnimation reference) {
        GestureAvailability availability = gestureAvailability(npc, reference);
        return availability == GestureAvailability.PARTIAL_BONES || availability == GestureAvailability.NO_MATCHING_BONES
            ? missingBones : Collections.emptyList();
    }

    @Override
    public List<String> missingBones(String geometry, DialogAnimation reference) {
        GestureAvailability availability = clipAvailability(geometry, reference);
        return availability == GestureAvailability.PARTIAL_BONES || availability == GestureAvailability.NO_MATCHING_BONES
            ? missingBones : Collections.emptyList();
    }

    private Animation clip(DialogAnimation reference) {
        AnimationFile file = GeckoLibCache.getInstance().getAnimations().get(new ResourceLocation(reference.file));
        return file == null ? null : file.getAnimation(reference.clip);
    }

    @Override
    public double durationTicks(DialogAnimation reference) {
        Animation animation = reference.isPresent() ? clip(reference) : null;
        return animation == null || animation.animationLength == null ? 0 : animation.animationLength;
    }

    private EntityCustomModel.DialoguePlayback play(EntityCustomModel model, Animation clip, boolean loop) {
        boolean movesHead = clip.boneAnimations.stream().anyMatch(bone -> {
            if (!bone.boneName.equals(model.headBoneName)) return false;
            VectorKeyFrameList<?> rotation = bone.rotationKeyFrames;
            return rotation != null && !(rotation.xKeyFrames.isEmpty() && rotation.yKeyFrames.isEmpty()
                && rotation.zKeyFrames.isEmpty());
        });
        return model.beginDialogueGesture(clip, resourceRevision(), movesHead, loop);
    }

    @Override
    public GestureStart playOnRenderedModel(EntityNPCInterface npc, EntityLivingBase rendered, DialogAnimation reference) {
        if (!(rendered instanceof EntityCustomModel) || !(npc.display instanceof IDataDisplay)
            || GeckoAddon.instance == null || !GeckoAddon.instance.supportEnabled) return GestureStart.SKIPPED;
        GestureAvailability availability = gestureAvailability(npc, reference);
        if (availability == GestureAvailability.MISSING_GEOMETRY || availability == GestureAvailability.MISSING_FILE) {
            return GestureStart.RETRY;
        }
        if (!availability.canPlay()) return GestureStart.SKIPPED;

        EntityCustomModel model = (EntityCustomModel) rendered;
        GeckoAddon.instance.geckoCopyData(npc, model);
        EntityCustomModel.DialoguePlayback playback = play(model, clip(reference), false);
        return playback == null ? GestureStart.SKIPPED : GestureStart.started(playback::stop, playback::canRebind);
    }

    @Override
    public GeckoAddonClient.GesturePreview preview(EntityNPCInterface npc, DialogAnimation reference) {
        return gestureAvailability(npc, reference).canPlay() ? previewModel(npc, null, false, reference, false) : null;
    }

    @Override
    public GeckoAddonClient.GesturePreview previewModel(EntityNPCInterface npc, ModelAsset override, boolean useMatchingResources,
                                                        DialogAnimation reference, boolean loop) {
        World world = npc == null ? Minecraft.getMinecraft().theWorld : npc.worldObj;
        String geometryId = override == null ? geometry(npc) : override.geometry;
        if (world == null || geometryId.isEmpty()) return null;
        ResourceLocation location = new ResourceLocation(geometryId);
        GeoModel geometry = GeckoLibCache.getInstance().getGeoModels().get(location);
        if (geometry == null || reference.isPresent() && !clipAvailability(geometryId, reference).canPlay()) return null;

        EntityCustomModel sample = new EntityCustomModel(world);
        if (hasModel(npc)) GeckoAddon.instance.geckoCopyData(npc, sample);
        sample.modelResLoc = location;
        if (override != null) {
            if (useMatchingResources && !override.texture.isEmpty()) sample.textureResLoc = new ResourceLocation(override.texture);
            if (!override.headBone.isEmpty() && !geometry.getBone(sample.headBoneName).isPresent()) sample.headBoneName = override.headBone;
        }
        sample.idleAnimName = sample.walkAnimName = "";
        sample.hurtTime = sample.deathTime = 0;
        sample.onGround = true;
        Animation selected = reference.isPresent() ? clip(reference) : null;
        if (selected != null) sample.animResLoc = new ResourceLocation(reference.file);
        EntityCustomModel.DialoguePlayback playback = selected == null ? null : play(sample, selected, loop);
        if (selected != null && playback == null) return null;
        refreshIndex();
        PreviewModelBounds measured = previewBounds.computeIfAbsent(geometry, PreviewModelBounds::measure);
        ModelProperties bounds = geometry.properties;
        double height = bounds == null ? sample.height : positive(bounds.getVisibleBoundsHeight(), sample.height);
        double width = bounds == null ? sample.width : positive(bounds.getVisibleBoundsWidth(), sample.width);
        double[] offset = bounds == null ? null : bounds.getVisibleBoundsOffset();
        double center = offset != null && offset.length > 1 && Double.isFinite(offset[1]) && offset[1] != 0 ? offset[1] : height / 2;
        final double fittedWidth = measured.valid() ? measured.width() : width;
        final double fittedHeight = measured.valid() ? measured.height() : height;
        final double fittedCenter = measured.valid() ? measured.center(1) : center;
        return new GeckoAddonClient.GesturePreview() {
            @Override
            public EntityLivingBase entity() {
                return sample;
            }

            @Override
            public void close() {
                if (playback != null) playback.stop();
                sample.releasePreviewTicker();
            }

            @Override public double durationTicks() {
                return selected == null || selected.animationLength == null ? 0 : selected.animationLength;
            }

            @Override public double width() { return fittedWidth; }
            @Override public double width(float yaw) { return measured.valid() ? measured.width(yaw) : width; }
            @Override public double height() { return fittedHeight; }
            @Override public double centerX() { return measured.valid() ? measured.center(0) : 0; }
            @Override public double centerY() { return fittedCenter; }
            @Override public double centerZ() { return measured.valid() ? measured.center(2) : 0; }
        };
    }

    private static double positive(Double value, double fallback) {
        return value != null && value > 0 && Double.isFinite(value) ? value : fallback;
    }
}
