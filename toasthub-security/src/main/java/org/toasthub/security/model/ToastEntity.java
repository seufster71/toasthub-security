package org.toasthub.security.model;

import java.io.Serializable;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import org.toasthub.core.general.api.View;

import com.fasterxml.jackson.annotation.JsonView;

@MappedSuperclass()
public class ToastEntity extends BaseEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	
	protected Text title;
	
	//Constructor
	public ToastEntity() {
		super();
	}
	
	// Getters/Setters
	@JsonView({View.Admin.class,View.System.class})
	@ManyToOne(targetEntity = Text.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "title_id")
	public Text getTitle() {
		return title;
	}
	public void setTitle(Text title) {
		this.title = title;
	}
	
	@Transient
	public void setTitleDefaultText(String defaultText){
		if (this.title != null) {
			this.title.setDefaultText(defaultText);
		} else {
			Text text = new Text();
			text.setDefaultText(defaultText);
			this.setTitle(text);
		}
	}
	
	@Transient
	public void setTitleMtext(Map<String,String> langMap){
		if (this.title != null) {
			this.title.setLangTexts(langMap);
		} else {
			Text text = new Text();
			text.setLangTexts(langMap);
			this.setTitle(text);
		}
	}
}
