/**
 * 
 */
package cn.bc.template.domain;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.FileCopyUtils;

import cn.bc.core.util.TemplateUtils;
import cn.bc.docs.domain.Attach;
import cn.bc.identity.domain.FileEntityImpl;

/**
 * 模板
 * 
 * @author lbj
 */
@Entity
@Table(name = "BC_TEMPLATE")
public class Template extends FileEntityImpl {
	private static final long serialVersionUID = 1L;
	private static Log logger = LogFactory.getLog(Template.class);

	/** 模板存储的子路径，开头末尾不要带"/" */
	public static String DATA_SUB_PATH = "template";

	private String orderNo;// 排序号
	private String code;// 编码
	private String path;// 物理文件保存的相对路径（相对于全局配置的app.data.realPath或app.data.subPath目录下的子路径，如"2011/bulletin/xxxx.doc"）
	private String subject;// 标题
	private String content;// 模板内容：文本和Html类型显示模板内容
	private boolean inner;// 内置：是、否，默认否
	private String desc;// 备注
	private int status;// 状态：0-正常,1-禁用
	private String version;// 版本号
	private String category;// 所属分类
	private TemplateType templateType;
	
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "TYPE_ID", referencedColumnName = "ID")
	public TemplateType getTemplateType() {
		return templateType;
	}

	public void setTemplateType(TemplateType templateType) {
		this.templateType = templateType;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Column(name = "STATUS_")
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	@Column(name = "VERSION_")
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Column(name = "ORDER_")
	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getContent() {
		return getContent(null);
	}

	/**
	 * 获取模板的文本字符串
	 * <p>
	 * 如果附件不是纯文本类型返回null,如果是自定义文本内容直接返回配置的内容,如果是纯文本附件返回附件的内容
	 * </p>
	 * 
	 * @param args
	 *            格式化参数，为空代表不执行格式化
	 * @return
	 */
	public String getContent(Map<String, Object> args) {
		// 不处理非纯文本类型
		if (!this.isPureText())
			return null;

		String txt = null;
		// 自定义文本
		if (this.templateType.getCode().equals("custom")) {
			txt = this.content;
		} else {
			// 读取文件流的字符串内容
			String p = Attach.DATA_REAL_PATH + "/" + DATA_SUB_PATH + "/"
					+ this.getPath();
			File file = new File(p);
			try {
				txt = FileCopyUtils.copyToString(new InputStreamReader(
						new FileInputStream(file), "UTF-8"));
			} catch (FileNotFoundException e) {
				logger.warn("getContent 附件文件不存在:file=" + p);
			} catch (IOException e) {
				logger.warn("getContent 读取模板文件错误:file=" + p + ",error="
						+ e.getMessage());
			}
		}

		if (txt == null || args == null)
			return txt;

		// 格式化处理
		return TemplateUtils.format(txt, args);
	}

	/**
	 * 获取模板的附件流
	 * <p>
	 * 如果是自定义文本内容返回由此内容构成的内存流,如果是附件类型返回附件流
	 * </p>
	 * 
	 * @return
	 */
	@Transient
	public InputStream getInputStream() {
		// 自定义文本,返回由此内容构成的字节流
		if (this.templateType.getCode().equals("custom")) {
			if (this.content == null)
				return null;
			return new ByteArrayInputStream(this.content.getBytes());
		}

		// 读取文件流并返回
		String p = Attach.DATA_REAL_PATH + "/" + DATA_SUB_PATH + "/"
				+ this.getPath();
		File file = new File(p);
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			logger.warn("getInputStream 附件文件不存在:file=" + p);
			return null;
		}
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Column(name = "INNER_")
	public boolean isInner() {
		return inner;
	}

	public void setInner(boolean inner) {
		this.inner = inner;
	}

	@Column(name = "DESC_")
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	/**
	 * 判断是否是纯文本型模板
	 * 
	 * @return
	 */
	@Transient
	public boolean isPureText() {
		return this.templateType.isPureText();
	}
}
