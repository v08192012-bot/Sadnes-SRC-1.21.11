package tech.sadnes.api.mixin.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resource.NamespaceResourceManager;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.sadnes.client.ClientInfo;
import tech.sadnes.client.util.crypto.AssetCrypto;

@Mixin(NamespaceResourceManager.class)
public class NamespaceResourceManagerMixin {
    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void sadnes$decryptResource(Identifier identifier, CallbackInfoReturnable<Optional<Resource>> callbackInfoReturnable) {
        if (ClientInfo.MOD_ID.equals(identifier.getNamespace())) {
            Optional<Resource> optionalResource = callbackInfoReturnable.getReturnValue();
            if (optionalResource != null && !optionalResource.isEmpty()) {
                callbackInfoReturnable.setReturnValue(Optional.of(sadnes$wrap(optionalResource.get())));
            }
        }
    }

    @Inject(method = "getAllResources", at = @At("RETURN"), cancellable = true)
    private void sadnes$decryptAllResources(Identifier identifier, CallbackInfoReturnable<List<Resource>> callbackInfoReturnable) {
        if (ClientInfo.MOD_ID.equals(identifier.getNamespace())) {
            List<Resource> resources = callbackInfoReturnable.getReturnValue();
            if (resources != null && !resources.isEmpty()) {
                List<Resource> resources2 = new ArrayList<>(resources.size());

                for (Resource resource : resources) {
                    resources2.add(sadnes$wrap(resource));
                }

                callbackInfoReturnable.setReturnValue(resources2);
            }
        }
    }

    @Inject(method = "findResources", at = @At("RETURN"), cancellable = true)
    private void sadnes$decryptFound(String string, Predicate<Identifier> predicate, CallbackInfoReturnable<Map<Identifier, Resource>> callbackInfoReturnable) {
        Map<Identifier, Resource> map = callbackInfoReturnable.getReturnValue();
        if (map != null && !map.isEmpty() && sadnes$isOurs(map.keySet())) {
            map.replaceAll((identifier, resource2) -> sadnes$wrap(resource2));
        }
    }

    @Inject(method = "findAllResources", at = @At("RETURN"), cancellable = true)
    private void sadnes$decryptFoundAll(String string, Predicate<Identifier> predicate, CallbackInfoReturnable<Map<Identifier, List<Resource>>> callbackInfoReturnable) {
        Map<Identifier, List<Resource>> map = callbackInfoReturnable.getReturnValue();
        if (map != null && !map.isEmpty() && sadnes$isOurs(map.keySet())) {
            map.replaceAll((identifier, resources) -> {
                List<Resource> resources2 = new ArrayList<>(resources.size());

                for (Resource resource2 : resources) {
                    resources2.add(sadnes$wrap(resource2));
                }

                return resources2;
            });
        }
    }

    private static boolean sadnes$isOurs(Set<Identifier> identifiers) {
        Iterator iterator2 = identifiers.iterator();
        return iterator2.hasNext() && ClientInfo.MOD_ID.equals(((Identifier)iterator2.next()).getNamespace());
    }

    private static Resource sadnes$wrap(Resource resource) {
        return new Resource(resource.getPack(), () -> AssetCrypto.wrap(resource.getInputStream()), resource::getMetadata);
    }
}
