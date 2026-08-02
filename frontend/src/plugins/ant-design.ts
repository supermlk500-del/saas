import type { App, Plugin } from 'vue'
import Alert from 'ant-design-vue/es/alert'
import Avatar from 'ant-design-vue/es/avatar'
import Badge from 'ant-design-vue/es/badge'
import Breadcrumb from 'ant-design-vue/es/breadcrumb'
import Button from 'ant-design-vue/es/button'
import Card from 'ant-design-vue/es/card'
import Col from 'ant-design-vue/es/col'
import Collapse from 'ant-design-vue/es/collapse'
import DatePicker from 'ant-design-vue/es/date-picker'
import Descriptions from 'ant-design-vue/es/descriptions'
import Drawer from 'ant-design-vue/es/drawer'
import Dropdown from 'ant-design-vue/es/dropdown'
import Empty from 'ant-design-vue/es/empty'
import Form from 'ant-design-vue/es/form'
import Image from 'ant-design-vue/es/image'
import Input from 'ant-design-vue/es/input'
import InputNumber from 'ant-design-vue/es/input-number'
import Layout from 'ant-design-vue/es/layout'
import Menu from 'ant-design-vue/es/menu'
import Modal from 'ant-design-vue/es/modal'
import PageHeader from 'ant-design-vue/es/page-header'
import Popconfirm from 'ant-design-vue/es/popconfirm'
import Result from 'ant-design-vue/es/result'
import Row from 'ant-design-vue/es/row'
import Select from 'ant-design-vue/es/select'
import Space from 'ant-design-vue/es/space'
import Spin from 'ant-design-vue/es/spin'
import Switch from 'ant-design-vue/es/switch'
import Table from 'ant-design-vue/es/table'
import Tag from 'ant-design-vue/es/tag'
import Tooltip from 'ant-design-vue/es/tooltip'
import TreeSelect from 'ant-design-vue/es/tree-select'

const components: Plugin[] = [
  Alert,
  Avatar,
  Badge,
  Breadcrumb,
  Button,
  Card,
  Col,
  Collapse,
  DatePicker,
  Descriptions,
  Drawer,
  Dropdown,
  Empty,
  Form,
  Image,
  Input,
  InputNumber,
  Layout,
  Menu,
  Modal,
  PageHeader,
  Popconfirm,
  Result,
  Row,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tag,
  Tooltip,
  TreeSelect,
]

export const installAntDesignComponents = (app: App) => {
  components.forEach((component) => {
    app.use(component)
  })
}
