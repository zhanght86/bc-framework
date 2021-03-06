/**
 *
 */
package cn.bc.identity.domain;

import cn.bc.core.EntityImpl;

import javax.persistence.*;
import java.util.Calendar;

/**
 * 文档实体基类：包含创建人信息和最后修改人信息
 * (主键使用 AUTO 生成策略)
 *
 * @author dragon
 */
@MappedSuperclass
public abstract class FileEntityImpl extends EntityImpl implements FileEntity<Long> {
	private static final long serialVersionUID = 1L;
	private Calendar fileDate;// 创建时间
	private ActorHistory author;// 创建人
	private Calendar modifiedDate;// 最后修改时间
	private ActorHistory modifier;// 最后修改人

	@Column(name = "FILE_DATE")
	@Temporal(TemporalType.TIMESTAMP)
	public Calendar getFileDate() {
		return fileDate;
	}

	public void setFileDate(Calendar fileDate) {
		this.fileDate = fileDate;
	}

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "AUTHOR_ID", referencedColumnName = "ID")
	public ActorHistory getAuthor() {
		return author;
	}

	public void setAuthor(ActorHistory author) {
		this.author = author;
	}

	@Column(name = "MODIFIED_DATE")
	@Temporal(TemporalType.TIMESTAMP)
	public Calendar getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(Calendar modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	@ManyToOne(fetch = FetchType.EAGER, optional = true)
	@JoinColumn(name = "MODIFIER_ID", referencedColumnName = "ID")
	public ActorHistory getModifier() {
		return modifier;
	}

	public void setModifier(ActorHistory modifier) {
		this.modifier = modifier;
	}
}