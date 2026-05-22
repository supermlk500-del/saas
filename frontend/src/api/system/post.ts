import request, { type RuoYiListResponse, type RuoYiResponse } from '@/utils/request'

export interface SysPostItem {
  postId: number
  postCode: string
  postName: string
  postSort?: number
  status?: string
  createTime?: string
  remark?: string
}

export interface SysPostListParams {
  pageNum?: number
  pageSize?: number
  postCode?: string
  postName?: string
  status?: string
}

export function listPosts(params: SysPostListParams) {
  return request<RuoYiListResponse<SysPostItem>>({
    url: '/system/post/list',
    method: 'get',
    params,
  })
}

export interface SysPostForm {
  postId?: number
  postCode: string
  postName: string
  postSort: number
  status?: string
  remark?: string
}

export function addPost(data: SysPostForm) {
  return request<RuoYiResponse>({
    url: '/system/post',
    method: 'post',
    data,
  })
}

export function updatePost(data: SysPostForm) {
  return request<RuoYiResponse>({
    url: '/system/post',
    method: 'put',
    data,
  })
}

export function deletePost(postId: number) {
  return request<RuoYiResponse>({
    url: `/system/post/${postId}`,
    method: 'delete',
  })
}
