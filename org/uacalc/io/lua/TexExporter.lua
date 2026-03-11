local input = io.read("*a")
local lat_table_sep = {"\\\\\n", " & "}

local function read_selected_ops_set()
    local set = {}
    local block = input:match("<uacalcExportOps>(.-)</uacalcExportOps>")
    if not block then
        return nil -- nil = žádný filtr => exportuj vše (backward compatible)
    end
    for op in block:gmatch("<op>(.-)</op>") do
        set[op] = true
    end
    return set
end

local selected_ops = read_selected_ops_set()

local function escape_lua_pattern(s)
    return (s:gsub("([%^%$%(%)%%%.%[%]%*%+%-%?])", "%%%1"))
end


-- Convert a table to a string with separators, up to a given depth
local function table_to_string(t, separators, level)
  if type(t) ~= "table" or level == 0 then
    return tostring(t)
  end

  local sep = separators[1] or ","  -- Use first separator
  local next_separators = {}        -- Slice off for next level
  for i = 2, #separators do
    next_separators[#next_separators + 1] = separators[i]
  end

  local parts = {}
  for i = 1, #t do
    local v = t[i]
    if type(v) == "table" and level > 1 then
      parts[#parts + 1] = table_to_string(v, next_separators, level - 1)
    else
      parts[#parts + 1] = tostring(v)
    end
  end

  return table.concat(parts, sep)
end


local function read_name()
    return input:match("<algName>(.-)</algName>") or ""
end

local function read_cardinality()
    return tonumber(input:match("<cardinality>(.-)</cardinality>")) or 0
end

local function read_op_names()
    local op_names = {}
    for match in input:gmatch("<opName>(.-)</opName>") do
        table.insert(op_names, match)
    end
    return op_names
end

local function read_arity(opname)
    return input:match("<opName>" ..escape_lua_pattern(opname) .. "</opName>%s*<arity>(.-)</arity>") or ""
end

local function read_operation(opname)
    local op_start = input:find("<opName>" .. escape_lua_pattern(opname) .."</opName>")
    if not op_start then
        return {}
    end
    local rest = input:sub(op_start)
    local m = rest:match("<intArray>(.-)</intArray>")
    if not m then
        return {}
    end

    local op_table = {}
    for line in m:gmatch("[^\r\n]+") do
        line = line:gsub("%s", "") -- remove all whitespace
        if line ~= "" then
            local string_row = line:match(">(.-)<")
            if string_row then
                local row = {}
                for num in string_row:gmatch("[^,]+") do
                    table.insert(row, num)
                end
                table.insert(op_table, row)
            end
        end
    end
    
    return op_table
end

local function read_constant(opname)
    local ans = "Not a constant!"
    if read_arity(opname) == "0" then
        ans = table_to_string(read_operation(opname), {""} ,2)
    end
    return ans
end

local function lat_op_table(opname)
    local op_table = read_operation(opname)
    local card = read_cardinality()
    local arity = tonumber(read_arity(opname)) or -1
    local ans = ""
    local t = {}

    for i = 0, card - 1 do
        table.insert(t, tostring(i))
    end

    -- Safety check
    if type(op_table) ~= "table" or #op_table == 0 then
        return "$\\text{Malformed operation: " .. opname .. "}$"
    end

    if arity == 1 then
        ans = "\n%op: ".. opname.. "\n$\\begin{array}{c|" .. string.rep("c", card) .. "}\n" ..
              opname .. " & " .. table.concat(t, " & ") .. "\\\\\n\\hline\n& "
    elseif arity == 2 then
        for i, row in ipairs(op_table) do
            table.insert(row, 1, tostring(i - 1))  -- Add row label
        end
        ans = "\n%op: ".. opname.. "\n$\\begin{array}{c|" .. string.rep("c", card) .. "}\n" ..
              opname .. " & " .. table.concat(t, " & ") .. "\\\\\n\\hline\n"
    else
        return "$\\text{Unsupported arity: " .. arity .. " for " .. opname .. "}$"
    end

    ans = ans .. table_to_string(op_table, lat_table_sep, 2) .. "\n\\end{array}$"
    return ans
end


-- Main processing
local alg_name = read_name()
local op_names = read_op_names()
local lat_tables = {}
local alg_type = {}

for i, op in ipairs(op_names) do
    if (selected_ops == nil) or selected_ops[op] then
        if read_arity(op) == "0" then
            table.insert(alg_type, read_constant(op))
        else
            table.insert(alg_type, op)
            table.insert(lat_tables, lat_op_table(op))
        end
    end
end
-- Output result
print("Algebra $\\mathbf{" .. alg_name .. "} = (" .. alg_name .. ", " .. table.concat(alg_type, ", ") .. ")$\n\\vspace{1em}\n")
print(table.concat(lat_tables, "\n,\\hspace{2ex}\n"))