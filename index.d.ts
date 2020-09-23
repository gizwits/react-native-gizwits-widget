type IError = 'Not Set Up';

interface IInit {
  token: string; // 用户的token
  uid: string;// 用户的uid
  appKey: string; // 机智云AppId，可在开发中心申请
  openUrl: string; // 机智云的Open API Url，开发者可根据自己的环境传入
  aepUrl: string; // 机智云的AEP API Url，开发者可根据自己的环境传入
  m2mUrl: string; // 目标设备连接的生产环境m2m地址
  m2mStageUrl: string; // 目标设备连接的测试环境m2m地址
  languageKey: string; // 语言参数，方便开发者实现多语言支持
  tintColor: string; // 主题颜色，开发者可根据自己的需要的主题传入，格式为#FFFFFF
}

interface ILanguageData {
  [propName: string]: string;
}

interface Langauge {
  zh: ILanguageData;
  [propName: string]: ILanguageData;
}

type Ttype = 'Boolean' | 'Number' | 'Enumeration';

interface IControlOption {
  value: boolean | string | number;
  image: string;
  name: string;
}

interface IControlData {
    id: string;
    attrs: string;
    type: Ttype;
    langauge: Langauge;
    editName: string;
    attrsIcon: string;
    option: IControlOption[];
}

interface IControlResult {
  data: IControlData; // 见上方类型定义
  error: string; //错误信息，字符串
}

interface ICondition {
  opt: '<=' | '>=' | '<' | '>' | '==' | '!=';
  value: boolean | string | number;
}

interface IStateContent {
  conditions: ICondition[];
  text: string;
  image: string;
}

interface IStateData {
  id: number;
  attrs: string;
  type: Ttype;
  editName: string;
  langauge: Langauge;
  content: IStateContent[];
}

interface IStateResult {
  data: IStateData; // 见上方类型定义
  error: string; //错误信息，字符串
}

interface ISceneData {
  id: string; // 手动场景id
  name: string; // 手动场景名称
  homeId: string;// 手动场景所在家庭id
  homeName: string;// 手动场景所在家庭名称
  icon: string; // 手动场景Icon
}

interface ISceneResult {
  data: IStateData; // 见上方类型定义
  error: string; //错误信息，字符串
}

export function setUpAppInfo(params: IInit);
export function getControlDeviceList(): IControlResult;
export function saveControlDeviceList(list: IControlData[], callback: (err: IError) => void);
export function getStateDeviceList(): IStateResult;
export function saveStateDeviceList(list: IStateData[], callback: (err: IError) => void);


export function getSceneList(): ISceneResult;
export function saveSceneList(list: ISceneData[], callback: (err: IError) => void);

export function clearAllData(callback: (err: IError) => void);
