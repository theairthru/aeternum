package dev.aeternum;
import com.mojang.blaze3d.vertex.VertexConsumer;
public record AlphaConsumer(VertexConsumer delegate,float alpha) implements VertexConsumer {
    public VertexConsumer addVertex(float x,float y,float z){delegate.addVertex(x,y,z);return this;}
    public VertexConsumer setColor(int r,int g,int b,int a){delegate.setColor(r,g,b,(int)(a*alpha));return this;}
    public VertexConsumer setColor(int argb){return setColor((argb>>>16)&255,(argb>>>8)&255,argb&255,(argb>>>24)&255);}
    public VertexConsumer setUv(float u,float v){delegate.setUv(u,v);return this;}
    public VertexConsumer setUv1(int u,int v){delegate.setUv1(u,v);return this;}
    public VertexConsumer setUv2(int u,int v){delegate.setUv2(u,v);return this;}
    public VertexConsumer setNormal(float x,float y,float z){delegate.setNormal(x,y,z);return this;}
    public VertexConsumer setLineWidth(float w){delegate.setLineWidth(w);return this;}
}
