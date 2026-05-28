# 词汇数据添加指南

## 快速添加词汇

### 方法1：手动添加JSON数据

编辑对应的JSON文件，按照以下格式添加词汇：

```json
{
  "word": "abandon",
  "phonetic": "/əˈbændən/",
  "meaning": "v. 放弃；遗弃",
  "example": "They had to abandon their car in the snow.",
  "exampleTranslation": "他们不得不把汽车遗弃在雪地里。",
  "level": 2,
  "category": "中考词汇",
  "grade": 7,
  "prefix": "ab-",
  "suffix": null,
  "root": "bandon",
  "rootMeaning": "控制"
}
```

### 方法2：批量导入

1. 准备CSV文件（格式：单词,音标,释义,例句,例句翻译）
2. 使用转换工具转为JSON格式
3. 复制到对应的JSON文件中

## 词汇分类

### 年级代码
- 1-6: 小学1-6年级
- 7: 中考词汇
- 8: 高考词汇
- 9: 四级词汇
- 10: 六级词汇

### 难度等级
- 1: 基础词汇（小学）
- 2: 初级词汇（初中）
- 3: 中级词汇（高中）
- 4: 中高级词汇（四级）
- 5: 高级词汇（六级及以上）

### 分类标签
- 小学一年级、小学二年级...
- 中考词汇、高考词汇
- 四级词汇、六级词汇
- 动词、名词、形容词（词性分类）

## 词根词缀分析

### 常见前缀
- un- (不)：unhappy, unable
- re- (再)：return, reuse
- dis- (不)：dislike, disappear
- pre- (前)：preview, prepare
- inter- (之间)：internet, international

### 常见后缀
- -tion (名词)：information, education
- -ly (副词)：quickly, slowly
- -ful (形容词)：beautiful, careful
- -less (否定)：careless, hopeless
- -er (人)：teacher, worker

### 常见词根
- vis/vid (看)：vision, video
- aud (听)：audio, audience
- dict (说)：dictionary, predict
- scrib/script (写)：describe, script
- port (携带)：import, export

## 数据验证

添加词汇后，请确保：
1. JSON格式正确
2. 所有必填字段都有值
3. 音标使用国际音标
4. 例句语法正确
5. 翻译准确

## 推荐工具

- **词典**：牛津词典、朗文词典
- **音标**：IPA国际音标表
- **例句**：例句应简单易懂
- **翻译**：确保翻译准确自然

## 注意事项

1. 避免重复词汇
2. 保持数据格式一致
3. 定期备份数据
4. 测试数据加载是否正常
