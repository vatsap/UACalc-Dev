local input = io.read("*a")


-- split lines:
-- first: options
-- second: the name of alg
-- third: labels of vertices
-- forth: vertices cooords
-- fifth: edges cooords

local lines = {}
for line in input:gmatch("([^\r\n]+)") do
    table.insert(lines, line)
end

-- check if the first line is an option line
local options_line = nil
if lines[1]:match("^Options:") then
    options_line = lines[1]
    table.remove(lines, 1)  -- remove the option line so it won't interfere with your existing parsing
end

local options = options_line:match("^Options:%s*(.*)$")


local alg_name = lines[1]:match("^%s*(.-)%s*$")

--matching vertices names
local inner = lines[2]:match("{(.*)}")
local vertices_names = {}
for item in inner:gmatch("[^&]+") do
    table.insert(vertices_names, item:match("^%s*(.-)%s*$"))  -- trim leading/trailing whitespace
end

--Little tunning depedning on number of vertices: 
--more vertices = denser code and smaller label font 
local vert_num = #vertices_names
local sep = "\n"
local font_size = "\\large"
if vert_num > 10 then
    sep = ""
end
if vert_num > 30 then
    font_size = ""
end
--end of tunning

--if the lattice is con lat, then | should be actual latex vert
for i, name in ipairs(vertices_names) do
    vertices_names[i] = name:gsub("%|", " \\vert ")
end
--

--loading vertices and edges
local vertices = load("return " .. lines[3])()
local edges = load("return " .. lines[4])()

--Preamble 
print("\\begin{figure}[h]\n\\centering\n\\begin{tikzpicture}[scale = 1]")

--tikz body
print("%edges\n")
for _, edge in ipairs(edges) do
    print("\\draw[very thick]\n(".. table.concat(edge[1], ", ").. ") -- ("..table.concat(edge[2], ",").. ");"..sep)
end

print("\n%vertices\n")
for i, v in ipairs(vertices) do
    if options == "labelinside" then
        print("\\filldraw[draw,fill]" .. "(" .. table.concat(v, ", ").. ")" .. "\nnode[rounded corners, fill=white, draw = black, very thick] ".. "{"..font_size.."$"..  vertices_names[i].. "$};"..sep)
    else
        print("\\filldraw[draw,fill]" .. "(" .. table.concat(v, ", ").. ")" .. "circle[radius= 3.5pt]\nnode[behind path, rounded corners, fill=black!15, below = 9pt] ".. "{"..font_size.."$"..  vertices_names[i].. "$};"..sep)
    end
end

--Postamble
print("\\end{tikzpicture}\n\\caption{".. alg_name .."}\n\\end{figure}")


