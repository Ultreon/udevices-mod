package dev.ultreon.devicesnext.mineos.gui;

import dev.ultreon.devicesnext.UDevicesMod;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

public class FrameBufferObject {
    private final TextureManager resourceManager;
    private final int width;
    private final int height;
    private int textureId;
    private final int frameBufferId;
    private final int depthBufferId;

    private final AbstractTexture texture;

    public FrameBufferObject(TextureManager textureManager, int width, int height) {
        this.resourceManager = textureManager;
        this.width = width;
        this.height = height;

        // Frame Buffer
        this.frameBufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);

        // Texture
        this.textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureId, 0);

        // Depth Buffer
        this.depthBufferId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBufferId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBufferId);

        glDrawBuffers(GL_COLOR_ATTACHMENT0);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Incomplete framebuffer");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        texture = new AbstractTexture() {
            @Override
            public void load(@NotNull ResourceManager resourceManager) {
                if (textureId == -1) return;
                FrameBufferObject.this.textureId = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, textureId);

                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureId, 0);
            }

            @Override
            public void releaseId() {
                glDeleteTextures(textureId);
                textureId = -1;
            }
        };

        textureManager.register(UDevicesMod.res("dynamic/framebuffers/fbo_" + frameBufferId), texture);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);
        glViewport(0, 0, width, height);
    }

    public static void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    public void delete() {
        glDeleteFramebuffers(frameBufferId);
        glDeleteRenderbuffers(depthBufferId);
    }
}
