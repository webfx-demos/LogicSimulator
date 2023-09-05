package com.orangomango.logicsim.core;

import com.orangomango.logicsim.Util;
import dev.webfx.platform.ast.AST;
import dev.webfx.platform.ast.AstArray;
import dev.webfx.platform.ast.AstObject;
import dev.webfx.platform.ast.ReadOnlyAstObject;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class Pin{
	private Rectangle2D rect;
	private volatile boolean on;
	private List<Pin> attached = new ArrayList<>();
	private boolean doInput;
	private int id;
	private boolean connected = true;
	private Gate owner;
	private static final Font FONT = new Font("sans-serif", 10);

	public static int PIN_ID = 0;

	public Pin(Gate owner, Rectangle2D r, boolean doIn){
		this.owner = owner;
		this.rect = r;
		this.doInput = doIn;
		this.id = PIN_ID++;
	}

	public void setOwner(Gate owner){
		this.owner = owner;
	}

	public Gate getOwner(){
		return this.owner;
	}

	public Pin(ReadOnlyAstObject json, boolean updatePinId){
		this.rect = new Rectangle2D(json.getObject("rect").getDouble("x"), json.getObject("rect").getDouble("y"), json.getObject("rect").getDouble("w"), json.getObject("rect").getDouble("h"));
		this.doInput = json.getBoolean("doInput");
		this.id = json.getInteger("id");
		if (updatePinId){
			PIN_ID = Math.max(PIN_ID, this.id+1);
		}
	}

	public AstObject getJSON(){
		AstObject json = AST.createObject();
		json.set("id", this.id);
		AstObject r = AST.createObject();
		r.set("x", this.rect.getMinX());
		r.set("y", this.rect.getMinY());
		r.set("w", this.rect.getWidth());
		r.set("h", this.rect.getHeight());
		json.set("rect", r);
		json.set("doInput", this.doInput);
		AstArray array = AST.createArray();
		for (Pin p : this.attached){
			array.push(p.getId());
		}
		json.set("attached", array);
		return json;
	}

	public void destroy(List<Gate> gates, List<Wire> wires, List<Wire> wiresToRemove){
		for (Gate g : gates){
			for (Pin p : g.getPins()){
				if (p.getAttachedPins().contains(this)){
					Wire w = Util.getWire(wires, p, this);
					wiresToRemove.add(w);
				}
			}
		}
		this.owner.getPins().remove(this);
		if (this.id == PIN_ID-1){
			PIN_ID--;
		}
	}

	public double getX(){
		return (this.rect.getMinX()+this.rect.getMaxX())/2;
	}

	public double getY(){
		return (this.rect.getMinY()+this.rect.getMaxY())/2;
	}

	public void setConnected(boolean c, boolean power){
		this.connected = c;
		if (!this.connected){
			setSignal(false, power);
		}
	}

	public boolean isConnected(){
		return this.connected;
	}

	public void move(double x, double y){
		this.rect = new Rectangle2D(this.rect.getMinX()+x, this.rect.getMinY()+y, this.rect.getWidth(), this.rect.getHeight());
	}

	public void setRect(Rectangle2D rect){
		this.rect = rect;
	}

	public Rectangle2D getRect(){
		return this.rect;
	}

	public boolean isInput(){
		return this.doInput;
	}

	public void attach(Pin o){
		if (!this.attached.contains(o)){
			this.attached.add(o);
		}
	}

	public List<Pin> getAttachedPins(){
		return this.attached;
	}

	private boolean hasOnPin(){
		for (Pin p : this.attached){
			if (p.isOn() && !p.isInput()){
				return true;
			}
		}
		return false;
	}

	public void updateAttachedPins(boolean power){
		if (!this.isInput()){ // Only output pins decide.
			if (this.on){
				for (Pin p : this.attached){
					p.setSignal(true, power);
					p.setConnected(this.connected, power);
				}
			} else {
				for (Pin p : this.attached){
					if (!p.hasOnPin()){
						p.setSignal(false, power);
					}
					p.setConnected(this.connected, power);
				}
			}
		}
	}

	public void setSignal(boolean on, boolean power){
		if ((!power || !this.connected) && on){
			return; // Power disabled
		}
		this.on = on;
	}

	public boolean isOn(){
		return this.on;
	}

	public int getId(){
		return this.id;
	}

	public void render(GraphicsContext gc, Color gateColor){
		gc.setFill(this.on ? Color.GREEN : Color.BLACK);
		gc.fillOval(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		gc.setStroke(this.doInput ? Color.BLUE : Color.RED);
		if (!this.connected) gc.setStroke(Color.PURPLE);
		gc.setLineWidth(1.5);
		gc.strokeOval(this.rect.getMinX(), this.rect.getMinY(), this.rect.getWidth(), this.rect.getHeight());
		if (Util.SHOW_PIN_ID){
			gc.setFill(Color.RED);
			gc.save();
			gc.setTextAlign(this.isInput() ? TextAlignment.RIGHT : TextAlignment.LEFT);
			gc.setFont(FONT);
			gc.fillText(Integer.toString(this.id), this.rect.getMinX()+(this.isInput() ? -8 : 23), this.rect.getMinY()+13);
			gc.restore();
		}
	}
}